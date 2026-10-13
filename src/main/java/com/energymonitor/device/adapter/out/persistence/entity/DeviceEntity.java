package com.energymonitor.device.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "device")
public class DeviceEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_device", nullable = false, length = 10)
    private String idDevice;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "appliance_type_id", nullable = false, length = 10)
    private String applianceTypeId;

    @Column(name = "location", length = 50)
    private String location;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "installation_date", nullable = false)
    private Instant installationDate;

    @Column(name = "device_code", nullable = false, length = 6, unique = true)
    private String deviceCode;

    /** SHA-256 of the key, hex; the key itself is never stored (device-007/008). */
    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    public String getIdDevice() {
        return idDevice;
    }

    public void setIdDevice(String idDevice) {
        this.idDevice = idDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApplianceTypeId() {
        return applianceTypeId;
    }

    public void setApplianceTypeId(String applianceTypeId) {
        this.applianceTypeId = applianceTypeId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(Instant installationDate) {
        this.installationDate = installationDate;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public void setApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }
}
