package com.energymonitor.device.adapter.out.security;

import com.energymonitor.device.application.port.out.DevicePersistencePort;
import org.springframework.stereotype.Component;

/**
 * Checks a device's code and API key against the stored hash of the key.
 */
@Component
public class DeviceCredentialAdapter {

    private final DevicePersistencePort devicePersistencePort;

    public DeviceCredentialAdapter(DevicePersistencePort devicePersistencePort) {
        this.devicePersistencePort = devicePersistencePort;
    }

    public boolean validateCredentials(String deviceCode, String apiKey) {
        if (deviceCode == null || apiKey == null) {
            return false;
        }
        return devicePersistencePort.findByDeviceCode(deviceCode)
                .map(d -> d.acceptsApiKey(apiKey))
                .orElse(false);
    }
}
