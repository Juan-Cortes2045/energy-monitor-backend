package com.energymonitor.measurement.application.exception;

/**
 * Base for the application-layer exceptions. Nothing technology-specific lives here.
 */
public class MeasurementApplicationException extends RuntimeException {

    public MeasurementApplicationException(String message) {
        super(message);
    }

    public MeasurementApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
