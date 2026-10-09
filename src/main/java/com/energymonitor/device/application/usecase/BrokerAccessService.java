package com.energymonitor.device.application.usecase;

import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.Device;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authentication and topic ACL for the MQTT broker.
 *
 * <p>Two kinds of client exist:
 *
 * <ul>
 *   <li><strong>The backend</strong>, with the credentials of its own configuration. It may only
 *       subscribe to and read the telemetry and status topics of every device.</li>
 *   <li><strong>A device</strong>, which logs in with its {@code device_code} as username and its
 *       {@code api_key} as password. It may only publish on its own two topics, so a device cannot
 *       impersonate another one or read anything.</li>
 * </ul>
 *
 * <p>Secrets are compared in constant time.
 *
 * <p>This service contains no Spring annotations.
 */
public class BrokerAccessService implements AuthorizeBrokerAccess {

    static final String TOPIC_PREFIX = "energy-monitor/devices/";

    /** A concrete device topic or the {@code +} subscription filter the backend uses. */
    private static final Pattern BACKEND_TOPIC =
            Pattern.compile(Pattern.quote(TOPIC_PREFIX) + "(\\+|[^/+#]+)/(telemetry|status)");

    /** The command topic of one device, which only the backend writes. */
    private static final Pattern COMMAND_TOPIC =
            Pattern.compile(Pattern.quote(TOPIC_PREFIX) + "[^/+#]+/command");

    private final DevicePersistencePort devices;
    private final String backendUsername;
    private final String backendPassword;

    public BrokerAccessService(DevicePersistencePort devices, String backendUsername, String backendPassword) {
        this.devices = devices;
        this.backendUsername = backendUsername;
        this.backendPassword = backendPassword;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return false;
        }
        if (isBackend(username)) {
            return !isBlank(backendPassword) && secretEquals(password, backendPassword);
        }
        return devices.findByDeviceCode(username)
                .map(device -> secretEquals(password, device.apiKey()))
                .orElse(false);
    }

    @Override
    public boolean authorize(String username, String topic, Access access) {
        if (isBlank(username) || isBlank(topic) || access == null) {
            return false;
        }
        if (isBackend(username)) {
            if (access == Access.WRITE) {
                return COMMAND_TOPIC.matcher(topic).matches();
            }
            return BACKEND_TOPIC.matcher(topic).matches();
        }
        Optional<Device> device = devices.findByDeviceCode(username);
        if (device.isEmpty()) {
            return false;
        }
        String own = TOPIC_PREFIX + device.get().idDevice() + "/";
        if (access != Access.WRITE) {
            // A module only reads (subscribes to) its own command topic.
            return topic.equals(own + "command");
        }
        return topic.equals(own + "telemetry") || topic.equals(own + "status");
    }

    private boolean isBackend(String username) {
        return !isBlank(backendUsername) && backendUsername.equals(username);
    }

    private static boolean secretEquals(String given, String expected) {
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
