package com.energymonitor.device.application.port.in;

/**
 * Use case: keep the connectivity state of a device up to date.
 */
public interface RecordDeviceConnectivity {

    /**
     * The device published telemetry or an {@code online} status.
     */
    void recordOnline(String deviceId, Integer signalStrength);

    /**
     * The device announced it is going away, or the broker published its last will.
     */
    void recordOffline(String deviceId);
}
