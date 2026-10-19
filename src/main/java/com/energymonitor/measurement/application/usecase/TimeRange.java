package com.energymonitor.measurement.application.usecase;

import java.time.Clock;
import java.time.Instant;

/**
 * Effective bounds of a time-ranged query. Package-private: a detail of the use cases,
 * not part of the module's contract.
 *
 * <p>The unbounded lower bound is {@code 1971-01-01} rather than {@link Instant#EPOCH}
 * because MySQL {@code TIMESTAMP} starts at {@code 1970-01-01 00:00:01} UTC and a bind
 * value below that is out of range for the column.
 */
final class TimeRange {

    static final Instant MIN_TIMESTAMP = Instant.parse("1971-01-01T00:00:00Z");

    private final Instant from;
    private final Instant to;

    private TimeRange(Instant from, Instant to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Resolves the effective bounds of a query: a {@code null} lower bound becomes
     * {@link #MIN_TIMESTAMP} and a {@code null} upper bound becomes the current instant.
     *
     * @param from  requested lower bound, may be {@code null}
     * @param to    requested upper bound, may be {@code null}
     * @param clock the clock for the default upper bound
     * @return the effective range
     */
    static TimeRange of(Instant from, Instant to, Clock clock) {
        return new TimeRange(from != null ? from : MIN_TIMESTAMP,
                to != null ? to : clock.instant());
    }

    Instant from() {
        return from;
    }

    Instant to() {
        return to;
    }
}
