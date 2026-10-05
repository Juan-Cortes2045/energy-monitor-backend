package com.energymonitor.measurement.adapter.in.web.dto;

import java.time.Instant;

/**
 * Response DTO for the consumption statistics of a device.
 *
 * @param deviceId           identifier of the measuring device
 * @param from               effective lower bound of the range
 * @param to                 effective upper bound of the range
 * @param measurementCount   number of readings in the range
 * @param averageActivePower average of the active power readings
 * @param maxActivePower     highest active power reading
 * @param consumedEnergy     energy consumed in the range (see the use case for the
 *                           approximation semantics)
 */
public record ConsumptionStatisticsResponse(String deviceId, Instant from, Instant to,
                                            long measurementCount, double averageActivePower,
                                            double maxActivePower, double consumedEnergy) {
}
