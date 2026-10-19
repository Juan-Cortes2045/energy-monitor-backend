package com.energymonitor.measurement.application.result;

import java.time.Instant;

/**
 * Result of a consumption statistics query: the aggregated readings of a device over a
 * time range.
 *
 * @param deviceId          identifier of the measuring device
 * @param from              effective lower bound of the range
 * @param to                effective upper bound of the range
 * @param measurementCount  number of readings in the range
 * @param averageActivePower average of the active power readings
 * @param maxActivePower    highest active power reading
 * @param consumedEnergy    energy consumed in the range, approximated as the delta between
 *                          the last and the first {@code storedEnergy} readings inside it
 */
public record ConsumptionStatisticsResult(String deviceId, Instant from, Instant to,
                                          long measurementCount, double averageActivePower,
                                          double maxActivePower, double consumedEnergy) {
}
