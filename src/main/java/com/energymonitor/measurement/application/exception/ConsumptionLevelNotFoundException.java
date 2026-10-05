package com.energymonitor.measurement.application.exception;

/**
 * A consumption level does not exist, or no level covers a given value.
 */
public class ConsumptionLevelNotFoundException extends MeasurementApplicationException {

    public ConsumptionLevelNotFoundException(String message) {
        super(message);
    }
}
