package com.energymonitor.measurement.application.command;

/**
 * Input of {@code GetLatestMeasurement}: the most recent reading of a device.
 *
 * @param deviceId identifier of the measuring device
 */
public record GetLatestMeasurementQuery(String deviceId) {
}
