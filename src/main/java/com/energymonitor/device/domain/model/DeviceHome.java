package com.energymonitor.device.domain.model;

import java.util.Objects;

/**
 * Association between a device and a home.
 */
public class DeviceHome {

    private final String deviceId;
    private final String homeId;

    public DeviceHome(String deviceId, String homeId) {
        this.deviceId = Preconditions.text(deviceId, 10, "deviceId");
        this.homeId = Preconditions.text(homeId, 10, "homeId");
    }

    public static DeviceHome of(String deviceId, String homeId) {
        return new DeviceHome(deviceId, homeId);
    }

    public String deviceId() {
        return deviceId;
    }

    public String homeId() {
        return homeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceHome that)) return false;
        return Objects.equals(deviceId, that.deviceId) && Objects.equals(homeId, that.homeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, homeId);
    }
}
