package com.energymonitor.device.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_home")
public class DeviceHomeEntity extends BaseAuditEntity {

    @EmbeddedId
    private DeviceHomeId id;

    @Column(name = "device_id", nullable = false, length = 10, insertable = false, updatable = false)
    private String deviceId;

    @Column(name = "home_id", nullable = false, length = 10, insertable = false, updatable = false)
    private String homeId;

    public DeviceHomeId getId() {
        return id;
    }

    public void setId(DeviceHomeId id) {
        this.id = id;
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
}
