package com.energymonitor.device.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DeviceHomeId implements Serializable {

    @Column(name = "device_id", nullable = false, length = 10)
    private String deviceId;

    @Column(name = "home_id", nullable = false, length = 10)
    private String homeId;

    public DeviceHomeId() {
    }

    public DeviceHomeId(String deviceId, String homeId) {
        this.deviceId = deviceId;
        this.homeId = homeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getHomeId() {
        return homeId;
    }

    public void setHomeId(String homeId) {
        this.homeId = homeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceHomeId that)) return false;
        return Objects.equals(deviceId, that.deviceId) && Objects.equals(homeId, that.homeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, homeId);
    }
}
