package com.energymonitor.device.domain.model;

/**
 * Kinds of appliance a device can monitor. {@link #value()} is the {@code name} column of the
 * {@code appliance_type} catalog (seeded by device-001 and device-006).
 */
public enum ApplianceType {
    REFRIGERATOR("refrigerator"),
    AIR_CONDITIONER("air_conditioner"),
    LIGHTING("lighting"),
    WASHING_MACHINE("washing_machine"),
    TELEVISION("television"),
    MICROWAVE("microwave"),
    COMPUTER("computer"),
    WATER_HEATER("water_heater"),
    OTHER("other");

    private final String value;

    ApplianceType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
