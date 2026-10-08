package com.energymonitor.recommendation.api;

/**
 * What a recommendation is about. Mirrors the {@code recommendation.type} ENUM column.
 */
public enum RecommendationType {
    /** A device keeps drawing power while nobody uses it (standby). */
    SAVING,
    /** A large share of the energy is used in the evening peak. */
    PEAK_HOURS,
    /** One device consumes clearly more than it used to. */
    HIGH_CONSUMPTION,
    /** The home consumed clearly more this week than the previous one. */
    HISTORICAL_COMPARISON,
    /** At the current pace the home will exceed its consumption limit. */
    THRESHOLD
}
