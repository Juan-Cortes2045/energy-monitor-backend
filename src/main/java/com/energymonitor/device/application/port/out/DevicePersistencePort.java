package com.energymonitor.device.application.port.out;

import com.energymonitor.device.domain.model.Device;
import java.util.Optional;

/**
 * Persistence port for devices.
 */
public interface DevicePersistencePort {

    void save(Device device);

    Optional<Device> findById(String deviceId);

    Optional<Device> findByDeviceCode(String deviceCode);
}
