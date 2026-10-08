package com.energymonitor.device.application.port.out;

import com.energymonitor.device.domain.model.DeviceHome;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for device-home associations.
 */
public interface DeviceHomePersistencePort {

    /**
     * Creates the association, or revives it when the same pair was unlinked before.
     */
    void save(DeviceHome deviceHome);

    Optional<DeviceHome> findByDeviceId(String deviceId);

    List<DeviceHome> findByHomeId(String homeId);

    /**
     * Soft-deletes the active association of a device, if any.
     */
    void unlink(String deviceId);
}
