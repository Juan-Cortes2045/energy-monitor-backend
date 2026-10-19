package com.energymonitor.measurement.api;

/**
 * Risk classification of a consumption value.
 *
 * <p>Placed in the public API rather than in the domain model because the class diagram
 * hands it to other contexts: {@code AlertFactory.createFromMeasurement(m, risk)} receives
 * it in the alert module, and recommendations reason about it too. Keeping it in
 * {@code measurement::api} lets those modules consume it without touching module internals.
 *
 * <p>Maps to the {@code ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')} column of
 * {@code consumption_level}.
 */
public enum RiskConsumption {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
