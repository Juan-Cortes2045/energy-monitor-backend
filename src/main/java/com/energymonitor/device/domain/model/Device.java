package com.energymonitor.device.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a physical measurement device.
 */
public class Device {

    private final String idDevice;
    private final String name;
    private final String applianceTypeId;
    private final String location;
    private final String description;
    private final Instant installationDate;
    private final String deviceCode;
    private final String apiKey;

    public Device(String idDevice, String name, String applianceTypeId, String location,
                  String description, Instant installationDate, String deviceCode, String apiKey) {
        this.idDevice = Preconditions.text(idDevice, 10, "idDevice");
        this.name = Preconditions.text(name, 50, "name");
        this.applianceTypeId = Preconditions.text(applianceTypeId, 10, "applianceTypeId");
        this.location = Preconditions.optionalText(location, 50, "location");
        this.description = Preconditions.optionalText(description, 200, "description");
        this.installationDate = Preconditions.notNull(installationDate, "installationDate");
        this.deviceCode = Preconditions.text(deviceCode, 6, "deviceCode");
        this.apiKey = Preconditions.text(apiKey, "apiKey");
    }

    public static Device create(String idDevice, String name, String applianceTypeId, String location,
                                String description, Instant installationDate, String deviceCode, String apiKey) {
        return new Device(idDevice, name, applianceTypeId, location, description, installationDate, deviceCode, apiKey);
    }

    /**
     * The same physical module linked again: new display data and a fresh API key, same
     * identity ({@code idDevice}, {@code deviceCode}).
     */
    public Device relinked(String name, String applianceTypeId, String location, Instant installationDate,
                           String apiKey) {
        return new Device(idDevice, name, applianceTypeId, location, description, installationDate,
                deviceCode, apiKey);
    }

    public String idDevice() {
        return idDevice;
    }

    public String name() {
        return name;
    }

    public String applianceTypeId() {
        return applianceTypeId;
    }

    public String location() {
        return location;
    }

    public String description() {
        return description;
    }

    public Instant installationDate() {
        return installationDate;
    }

    public String deviceCode() {
        return deviceCode;
    }

    public String apiKey() {
        return apiKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device that)) return false;
        return Objects.equals(idDevice, that.idDevice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDevice);
    }
}
