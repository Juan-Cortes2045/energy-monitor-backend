package com.energymonitor.alert.adapter.out.client;

import com.energymonitor.alert.application.port.out.HomeLookupPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Placeholder home resolution while the Devices module does not exist.
 *
 * <p>The {@code DeviceHome} relationship belongs to the Devices bounded context, which
 * has not been built yet. Until it exposes a {@code DeviceApi} capable of resolving the
 * home of a device, this adapter resolves nothing and
 * {@code RegisterThresholdAlertService} skips the reading instead of raising an alert
 * against a guessed home. Replace the body with a real cross-module call when the device
 * module lands; the port and the use case stay untouched.
 */
@Component
public class DeviceHomeLookupAdapter implements HomeLookupPort {

    @Override
    public Optional<String> findHomeIdByDeviceId(String deviceId) {
        return Optional.empty();
    }
}
