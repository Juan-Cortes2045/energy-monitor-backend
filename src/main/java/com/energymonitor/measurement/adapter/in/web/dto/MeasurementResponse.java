package com.energymonitor.measurement.adapter.in.web.dto;

import java.time.Instant;

/**
 * Response DTO for a measurement.
 *
 * @param idMeasurement identifier
 * @param deviceId      identifier of the measuring device
 * @param dateTime      business timestamp of the reading
 * @param voltage       voltage reading
 * @param current       current reading
 * @param activePower   active power reading
 * @param storedEnergy  accumulated energy reading
 */
public record MeasurementResponse(String idMeasurement, String deviceId, Instant dateTime,
                                  double voltage, double current, double activePower, double storedEnergy) {
}
