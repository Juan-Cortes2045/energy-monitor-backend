package com.energymonitor.measurement.application.exception;

/**
 * A measurement (or any measurement of a device) does not exist.
 */
public class MeasurementNotFoundException extends MeasurementApplicationException {

    public MeasurementNotFoundException(String message) {
        super(message);
    }
}
