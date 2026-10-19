package com.energymonitor.device.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a physical measurement device.
 *
 * <p>The API key (the device's MQTT password) is never kept in clear: the factories that receive a
 * fresh key store only its {@link ApiKeyHash}, and the key itself is returned to the owner once,
 * by the use case that generated it.
 */
public class Device {

    private final String idDevice;
    private final String name;
    private final String applianceTypeId;
    private final String location;
    private final String description;
    private final Instant installationDate;
    private final String deviceCode;
    private final String apiKeyHash;

    /**
     * Rehydrates a device as stored.
     *
     * @param apiKeyHash the stored {@link ApiKeyHash} of its key, never the key itself
     */
    public Device(String idDevice, String name, String applianceTypeId, String location,
                  String description, Instant installationDate, String deviceCode, String apiKeyHash) {
        this.idDevice = Preconditions.text(idDevice, 10, "idDevice");
        this.name = Preconditions.text(name, 50, "name");
        this.applianceTypeId = Preconditions.text(applianceTypeId, 10, "applianceTypeId");
        this.location = Preconditions.optionalText(location, 50, "location");
        this.description = Preconditions.optionalText(description, 200, "description");
        this.installationDate = Preconditions.notNull(installationDate, "installationDate");
        this.deviceCode = Preconditions.text(deviceCode, 6, "deviceCode");
        this.apiKeyHash = Preconditions.text(apiKeyHash, ApiKeyHash.LENGTH, "apiKeyHash");
    }

    /**
     * A new device.
     *
     * @param apiKey its fresh key in clear; only its hash is kept
     */
    public static Device create(String idDevice, String name, String applianceTypeId, String location,
                                String description, Instant installationDate, String deviceCode, String apiKey) {
        return new Device(idDevice, name, applianceTypeId, location, description, installationDate, deviceCode,
                ApiKeyHash.of(apiKey));
    }

    /**
     * The same physical module linked again: new display data and a fresh API key, same
     * identity ({@code idDevice}, {@code deviceCode}). Only the hash of {@code apiKey} is kept.
     */
    public Device relinked(String name, String applianceTypeId, String location, Instant installationDate,
                           String apiKey) {
        return new Device(idDevice, name, applianceTypeId, location, description, installationDate,
                deviceCode, ApiKeyHash.of(apiKey));
    }

    /**
     * New display data (what it measures, its name and room). Identity, key and link date stay.
     */
    public Device edited(String name, String applianceTypeId, String location) {
        return new Device(idDevice, name, applianceTypeId, location, description, installationDate,
                deviceCode, apiKeyHash);
    }

    /**
     * A fresh API key for the same module, e.g. to write it again together with a new Wi-Fi
     * network. It is not a new link: the link date stays. Only the hash of {@code apiKey} is kept.
     */
    public Device withApiKey(String apiKey) {
        return new Device(idDevice, name, applianceTypeId, location, description, installationDate,
                deviceCode, ApiKeyHash.of(apiKey));
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

    /** @return the stored hash of the key; the key itself cannot be recovered */
    public String apiKeyHash() {
        return apiKeyHash;
    }

    /**
     * Whether a key presented by a module (as its MQTT password) is this device's key.
     *
     * @param candidate the key received, in clear
     * @return {@code true} when it matches, compared in constant time
     */
    public boolean acceptsApiKey(String candidate) {
        return ApiKeyHash.matches(candidate, apiKeyHash);
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
