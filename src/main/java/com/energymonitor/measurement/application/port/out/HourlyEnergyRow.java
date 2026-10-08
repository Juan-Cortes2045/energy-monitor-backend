package com.energymonitor.measurement.application.port.out;

import java.time.Instant;

/**
 * Readings of one device inside one UTC hour, aggregated by the store.
 *
 * @param hourStart        start of the hour
 * @param averagePower     mean active power, W
 * @param minStoredEnergy  lowest cumulative counter in the hour, kWh
 * @param maxStoredEnergy  highest cumulative counter in the hour, kWh
 */
public record HourlyEnergyRow(String deviceId, Instant hourStart, double averagePower,
                              double minStoredEnergy, double maxStoredEnergy) {
}
