package com.energymonitor.measurement.api;

import java.time.Instant;

/**
 * A reading the measurement service stored, heard from its {@code measurement.recorded} message
 * and published again in memory, so the alert module evaluates it as it always did.
 *
 * @param idMeasurement identifier of the recorded measurement
 * @param deviceId      identifier of the measuring device
 * @param dateTime      business timestamp of the reading
 * @param voltage       voltage reading
 * @param current       current reading
 * @param activePower   active power reading
 * @param storedEnergy  accumulated energy reading
 */
public record MeasurementRecorded(String idMeasurement, String deviceId, Instant dateTime,
                                  double voltage, double current, double activePower, double storedEnergy) {
}
