package com.energymonitor.measurement.application.command;

import java.time.Instant;

/**
 * Input of {@code RegisterMeasurement}: records a single electrical reading of a device.
 *
 * @param deviceId     identifier of the measuring device, {@code VARCHAR(10)}
 * @param dateTime     business timestamp of the reading; when {@code null} the server clock is used
 * @param voltage      voltage reading, must not be negative
 * @param current      current reading, must not be negative
 * @param activePower  active power reading, must not be negative
 * @param storedEnergy accumulated energy reading, must not be negative
 */
public record RegisterMeasurementCommand(String deviceId, Instant dateTime,
                                         double voltage, double current, double activePower, double storedEnergy) {
}
