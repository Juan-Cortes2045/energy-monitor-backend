package com.energymonitor.alert.api;

/**
 * Lifecycle of an alert.
 *
 * <p>Placed in the public API because it rides inside {@code AlertDto}.
 *
 * <p>Maps to the {@code ENUM('PENDING', 'RESOLVED')} column of {@code alert}.
 */
public enum AlertStatus {
    PENDING,
    RESOLVED
}
