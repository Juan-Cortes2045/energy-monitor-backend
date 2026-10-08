package com.energymonitor.device.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import com.energymonitor.device.domain.model.DeviceStatus;

@Entity
@Table(name = "device_status_log")
public class DeviceStatusLogEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_device_status", nullable = false, length = 10)
    private String idDeviceStatus;

    @Column(name = "device_id", length = 10)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('ONLINE','OFFLINE')")
    private DeviceStatus status;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    public String getIdDeviceStatus() {
        return idDeviceStatus;
    }

    public void setIdDeviceStatus(String idDeviceStatus) {
        this.idDeviceStatus = idDeviceStatus;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public Integer getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}
