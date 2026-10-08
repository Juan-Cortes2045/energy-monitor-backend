package com.energymonitor.recommendation.domain.model;

import java.time.Instant;

/**
 * Consumption of one device in one UTC hour, as the measurement module computes it.
 *
 * @param averagePower mean active power, W
 * @param energy       energy consumed in the hour, kWh
 */
public record HourSample(String deviceId, Instant hourStart, double averagePower, double energy) {
}
