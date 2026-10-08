package com.energymonitor.home.api;

/**
 * Which consumption limit of a home is the one the owner set. Only one is active at a time; the
 * other is derived from it (30 days per month) and shown as calculated.
 */
public enum LimitPeriod {
    DAILY,
    MONTHLY
}
