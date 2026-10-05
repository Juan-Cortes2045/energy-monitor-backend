package com.energymonitor.measurement.application.command;

import java.time.Instant;

/**
 * Input of {@code ListMeasurements}: the consumption history of a device, optionally
 * bounded to a time range.
 *
 * @param deviceId identifier of the measuring device
 * @param from     inclusive lower bound of {@code dateTime}; unbounded when {@code null}
 * @param to       inclusive upper bound of {@code dateTime}; defaults to now when {@code null}
 */
public record ListMeasurementsQuery(String deviceId, Instant from, Instant to) {
}
