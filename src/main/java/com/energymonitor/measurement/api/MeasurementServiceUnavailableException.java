package com.energymonitor.measurement.api;

/**
 * The measurement service did not answer in time, or answered with an error, and nothing was kept
 * to fall back on. Each caller degrades in its own way: a limit alert waits for the next reading,
 * a recommendation for the next evaluation, and a reading that cannot be classified goes back to
 * the broker to be retried.
 */
public class MeasurementServiceUnavailableException extends RuntimeException {

    public MeasurementServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
