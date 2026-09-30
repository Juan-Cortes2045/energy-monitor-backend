package com.energymonitor.home.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root of a home.
 *
 * <p>A home represents a physical space where measurement devices are installed
 * and users collaborate. It owns its identity, location, access code and creation date.
 *
 * <p><strong>Thresholds:</strong> The diagram of classes shows {@code updateThresholds(daily, monthly)}
 * on {@code Home}. In this model, {@link HomeThresholds} is a separate aggregate (1:1 composition)
 * because it has its own identifier, its own lifecycle invariants (HOME-INV-005/006/007) and its
 * own persistence row. The threshold update therefore lives in {@link HomeThresholds#update}.
 * The use case {@code UpdateHomeThresholdsService} orchestrates both aggregates.
 *
 * <p><strong>Devices:</strong> The diagram shows {@code addDevice}, {@code removeDevice} and
 * {@code getAllDevices} on {@code Home}. These are <em>not</em> implemented here: {@code Device}
 * and {@code DeviceHome} belong to the {@code device} bounded context, and {@code home} declares
 * {@code @ApplicationModule(allowedDependencies = {})}. The device module resolves the
 * {@code DeviceHome} relationship with {@code home_id} directly.
 *
 * <p><strong>Users:</strong> The diagram shows {@code addUser(User, Role)} and {@code removeUser(User)}.
 * They are <em>not</em> implemented on {@code Home}: the membership is a separate aggregate
 * ({@link UserHome}) with its own identity and lifecycle. The equivalent operations are
 * the factories {@link UserHome#owner(String, String)} and {@link UserHome#member(String, String)},
 * orchestrated by the use cases {@code CreateHomeService} and {@code JoinHomeService}.
 * The {@code userId} is a {@code String} because {@code User} belongs to the {@code security}
 * bounded context and cannot be imported.
 *
 * <p>Maps to the {@code home} table.
 */
public class Home {

    private final String idHome;
    private final String name;
    private final String homeTypeId;
    private final String address;
    private final String accessCode;
    private final String description;
    private final Instant creationDate;

    /**
     * Rehydrates or creates a home. Prefer {@link #create} for new homes.
     *
     * @param idHome       identifier, {@code VARCHAR(10)}
     * @param name         display name, {@code VARCHAR(50)}
     * @param homeTypeId   identifier of the home type, {@code VARCHAR(10)}
     * @param address      physical address, {@code VARCHAR(200)}
     * @param accessCode   unique join code, {@code VARCHAR(8)} (HOME-INV-009)
     * @param description  optional description, {@code VARCHAR(200)}
     * @param creationDate when the home was created
     */
    public Home(String idHome, String name, String homeTypeId, String address,
                String accessCode, String description, Instant creationDate) {
        this.idHome = Preconditions.text(idHome, 10, "idHome");
        this.name = Preconditions.text(name, 50, "name");
        this.homeTypeId = Preconditions.text(homeTypeId, 10, "homeTypeId");
        this.address = Preconditions.text(address, 200, "address");
        this.accessCode = Preconditions.text(accessCode, 8, "accessCode");
        this.description = Preconditions.optionalText(description, 200, "description");
        this.creationDate = Preconditions.notNull(creationDate, "creationDate");
    }

    /**
     * Creates a new home.
     *
     * @param idHome       identifier
     * @param name         display name
     * @param homeTypeId   identifier of the home type
     * @param address      physical address
     * @param accessCode   unique join code, exactly 8 characters (HOME-INV-009)
     * @param description  optional description
     * @param creationDate when the home was created
     * @return a new home
     */
    public static Home create(String idHome, String name, String homeTypeId, String address,
                              String accessCode, String description, Instant creationDate) {
        return new Home(idHome, name, homeTypeId, address, accessCode, description, creationDate);
    }

    /** @return the identifier */
    public String idHome() {
        return idHome;
    }

    /** @return the display name */
    public String name() {
        return name;
    }

    /** @return identifier of the home type */
    public String homeTypeId() {
        return homeTypeId;
    }

    /** @return the physical address */
    public String address() {
        return address;
    }

    /** @return the unique join code */
    public String accessCode() {
        return accessCode;
    }

    /** @return the optional description */
    public String description() {
        return description;
    }

    /** @return when the home was created */
    public Instant creationDate() {
        return creationDate;
    }

    /**
     * Whether the given access code matches this home.
     *
     * @param code the code to test
     * @return {@code true} when the code matches
     */
    public boolean matchesAccessCode(String code) {
        return accessCode.equals(code);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Home that)) {
            return false;
        }
        return idHome.equals(that.idHome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idHome);
    }

    @Override
    public String toString() {
        return "Home{idHome='" + idHome + "', name='" + name + "', accessCode='" + accessCode + "'}";
    }
}
