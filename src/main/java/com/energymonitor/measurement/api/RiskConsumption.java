package com.energymonitor.measurement.api;

/**
 * Risk classification of a consumption value.
 *
 * <p>Placed in the public API rather than in the domain model because the class diagram
 * hands it to other contexts: the consumption levels carry it, and recommendations reason
 * about it. Keeping it in
 * {@code measurement::api} lets those modules consume it without touching module internals.
 *
 * <p>The names of the {@code consumption_level} catalogue of the measurement service.
 */
public enum RiskConsumption {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
