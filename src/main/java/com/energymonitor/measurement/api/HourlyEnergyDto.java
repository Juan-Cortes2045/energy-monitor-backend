package com.energymonitor.measurement.api;

import java.time.Instant;

/**
 * Energy of one device in one UTC hour.
 *
 * @param hourStart    start of the hour
 * @param averagePower mean active power in the hour, W
 * @param energy       energy consumed in the hour, kWh
 */
public record HourlyEnergyDto(String deviceId, Instant hourStart, double averagePower, double energy) {
}
