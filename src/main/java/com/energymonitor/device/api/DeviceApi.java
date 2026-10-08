package com.energymonitor.device.api;

import java.util.List;
import java.util.Optional;

/**
 * Public API of the Devices bounded context.
 */
public interface DeviceApi {

    Optional<DeviceDto> findByDeviceId(String deviceId);

    Optional<DeviceDto> findByDeviceCode(String deviceCode);

    boolean validateCredentials(String deviceCode, String apiKey);

    Optional<String> getHomeId(String deviceId);

    /**
     * The devices currently linked to a home.
     */
    List<DeviceDto> findByHome(String homeId);

    /**
     * Records that the device was heard from or announced a state change.
     *
     * <p>Only transitions create a row in {@code device_status_log}: repeated {@code ONLINE}
     * reports refresh the {@code last_seen} of the current row. The time of the report is the
     * server clock, never the device timestamp, so replayed offline samples do not move it.
     *
     * @param deviceId the device
     * @param status   the reported state
     * @param rssi     signal strength in dBm, may be null
     */
    void recordConnectivity(String deviceId, ConnectivityStatus status, Integer rssi);
}
