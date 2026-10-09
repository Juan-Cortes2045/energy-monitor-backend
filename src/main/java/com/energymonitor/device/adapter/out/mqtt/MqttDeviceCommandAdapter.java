package com.energymonitor.device.adapter.out.mqtt;

import com.energymonitor.device.application.port.out.DeviceCommandPort;
import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes module commands on the broker with the backend's credentials.
 *
 * <p>Commands are rare (a module removed or linked again), so each one opens a short connection
 * of its own instead of keeping a client alive. The client id is the backend's with a
 * {@code -commands} suffix, different from the subscribers', so it never takes their session.
 * Messages are retained at QoS 1: a module that is off gets the command as soon as it connects.
 */
@Component
public class MqttDeviceCommandAdapter implements DeviceCommandPort {

    private static final Logger LOG = LoggerFactory.getLogger(MqttDeviceCommandAdapter.class);
    static final String TOPIC = "energy-monitor/devices/%s/command";
    static final String UNLINKED = "{\"cmd\":\"unlink\"}";

    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;

    public MqttDeviceCommandAdapter(@Value("${mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
                                    @Value("${mqtt.client-id:energy-monitor-backend}") String clientId,
                                    @Value("${mqtt.username:}") String username,
                                    @Value("${mqtt.password:}") String password) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId + "-commands";
        this.username = username;
        this.password = password;
    }

    @Override
    public void sendUnlinked(String deviceId) {
        publish(deviceId, UNLINKED.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void clearCommands(String deviceId) {
        // An empty retained message deletes the retained one.
        publish(deviceId, new byte[0]);
    }

    private synchronized void publish(String deviceId, byte[] payload) {
        String topic = TOPIC.formatted(deviceId);
        try (MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence())) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setAutomaticReconnect(false);
            if (!username.isBlank()) {
                options.setUserName(username);
            }
            if (!password.isBlank()) {
                options.setPassword(password.toCharArray());
            }
            client.connect(options);
            client.publish(topic, payload, 1, true);
            client.disconnect();
            LOG.info("Command {} sent to {}", payload.length == 0 ? "clear" : "unlink", topic);
        } catch (MqttException e) {
            // The api key is already revoked: the module still ends up offering Bluetooth once the
            // broker rejects it. The command only makes it immediate.
            LOG.warn("Command to {} could not be sent: {}", topic, e.getMessage());
        }
    }
}
