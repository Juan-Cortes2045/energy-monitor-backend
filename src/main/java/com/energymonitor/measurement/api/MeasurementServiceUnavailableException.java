package com.energymonitor.measurement.api;

/**
 * The measurement service did not answer in time, or answered with an error, and nothing was kept
 * to fall back on. A recommendation waits for the next evaluation.
 */
public class MeasurementServiceUnavailableException extends RuntimeException {

    public MeasurementServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
