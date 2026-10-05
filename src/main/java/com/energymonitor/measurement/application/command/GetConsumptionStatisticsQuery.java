package com.energymonitor.measurement.application.command;

import java.time.Instant;

/**
 * Input of {@code GetConsumptionStatistics}: aggregates the readings of a device over a
 * time range.
 *
 * @param deviceId identifier of the measuring device
 * @param from     inclusive lower bound of {@code dateTime}; unbounded when {@code null}
 * @param to       inclusive upper bound of {@code dateTime}; defaults to now when {@code null}
 */
public record GetConsumptionStatisticsQuery(String deviceId, Instant from, Instant to) {
}
