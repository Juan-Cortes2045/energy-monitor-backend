package com.energymonitor.measurement.api;

import java.time.Instant;

/**
 * Read view of a measurement for other modules.
 *
 * @param idMeasurement identifier
 * @param deviceId      identifier of the measuring device
 * @param dateTime      business timestamp of the reading
 * @param voltage       voltage reading
 * @param current       current reading
 * @param activePower   active power reading
 * @param storedEnergy  accumulated energy reading
 */
public record MeasurementDto(String idMeasurement, String deviceId, Instant dateTime,
                             double voltage, double current, double activePower, double storedEnergy) {
}
