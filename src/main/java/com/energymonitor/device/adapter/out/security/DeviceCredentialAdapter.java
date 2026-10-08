package com.energymonitor.device.adapter.out.security;

import com.energymonitor.device.application.port.out.DevicePersistencePort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

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
                .map(d -> MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8),
                        d.apiKey().getBytes(StandardCharsets.UTF_8)))
                .orElse(false);
    }
}
