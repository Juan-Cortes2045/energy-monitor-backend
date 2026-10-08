package com.energymonitor.device.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Historical record of device connectivity status.
 */
public class DeviceStatusLog {

    private final String idDeviceStatus;
    private final String deviceId;
    private final DeviceStatus status;
    private final Integer signalStrength;
    private final Instant lastSeen;

    public DeviceStatusLog(String idDeviceStatus, String deviceId, DeviceStatus status,
                           Integer signalStrength, Instant lastSeen) {
        this.idDeviceStatus = Preconditions.text(idDeviceStatus, 10, "idDeviceStatus");
        this.deviceId = Preconditions.optionalText(deviceId, 10, "deviceId");
        this.status = Preconditions.notNull(status, "status");
        this.signalStrength = signalStrength;
        this.lastSeen = Preconditions.notNull(lastSeen, "lastSeen");
    }

    public static DeviceStatusLog create(String idDeviceStatus, String deviceId, DeviceStatus status,
                                         Integer signalStrength, Instant lastSeen) {
        return new DeviceStatusLog(idDeviceStatus, deviceId, status, signalStrength, lastSeen);
    }

    public String idDeviceStatus() {
        return idDeviceStatus;
    }

    public String deviceId() {
        return deviceId;
    }

    public DeviceStatus status() {
        return status;
    }

    public Integer signalStrength() {
        return signalStrength;
    }

    public Instant lastSeen() {
        return lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceStatusLog that)) return false;
        return Objects.equals(idDeviceStatus, that.idDeviceStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDeviceStatus);
    }
}
