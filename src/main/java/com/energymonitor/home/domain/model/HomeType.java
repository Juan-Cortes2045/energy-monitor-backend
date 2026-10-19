package com.energymonitor.home.domain.model;

import java.util.Objects;

/**
 * Catalog of home types (house, apartment, studio, other).
 *
 * <p>A read-only reference entity. It carries no behavior beyond identity and name.
 *
 * <p>Maps to the {@code home_type} table.
 */
public class HomeType {

    private final String idHomeType;
    private final String name;

    /**
     * @param idHomeType identifier, {@code VARCHAR(10)}
     * @param name       display name, {@code VARCHAR(50)}, unique
     */
    public HomeType(String idHomeType, String name) {
        this.idHomeType = Preconditions.text(idHomeType, 10, "idHomeType");
        this.name = Preconditions.text(name, 50, "name");
    }

    /** @return the identifier */
    public String idHomeType() {
        return idHomeType;
    }

    /** @return the display name */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HomeType that)) {
            return false;
        }
        return idHomeType.equals(that.idHomeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idHomeType);
    }

    @Override
    public String toString() {
        return "HomeType{idHomeType='" + idHomeType + "', name='" + name + "'}";
    }
}
