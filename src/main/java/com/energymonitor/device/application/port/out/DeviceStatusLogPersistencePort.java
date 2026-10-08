package com.energymonitor.device.application.port.out;

import com.energymonitor.device.domain.model.DeviceStatusLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for device status logs.
 */
public interface DeviceStatusLogPersistencePort {

    void save(DeviceStatusLog log);

    /**
     * The most recent log of the device, which holds its current connectivity state.
     */
    Optional<DeviceStatusLog> findLatestByDeviceId(String deviceId);

    /**
     * Refreshes the signal strength and last-seen time of an existing log.
     */
    void touch(String idDeviceStatus, Integer signalStrength, Instant lastSeen);

    /**
     * Current {@code ONLINE} logs that have not been refreshed since {@code cutoff}.
     */
    List<DeviceStatusLog> findOnlineNotSeenSince(Instant cutoff);
}
