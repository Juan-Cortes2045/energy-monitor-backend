package com.energymonitor.measurement.api;

import java.time.Instant;

/**
 * Application event published after a measurement is persisted.
 *
 * <p>This is the integration seam the class diagram models as the Observer pattern:
 * {@code AlertObserver} and {@code RecommendationObserver} belong to the alert and
 * recommendation bounded contexts, so they are expected to subscribe to this event
 * (e.g. through a Spring Modulith event listener) instead of registering in-process
 * observers here.
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
