package com.energymonitor.alert.api;

/**
 * What triggered an alert.
 *
 * <p>Placed in the public API because it rides inside {@code AlertRaised} and
 * {@code AlertDto}, which other modules consume.
 *
 * <p>Maps to the {@code ENUM('THRESHOLD', 'CONNECTIVITY')} column of {@code alert}.
 */
public enum AlertType {
    /** A consumption reading crossed into a HIGH or CRITICAL consumption level. */
    THRESHOLD,
    /** A device stopped reporting. */
    CONNECTIVITY
}
