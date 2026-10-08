package com.energymonitor.measurement.application.exception;

/**
 * The home does not exist or the caller is not a member of it. Both cases look the same to the
 * caller, so home identifiers cannot be probed.
 */
public class HomeNotAccessibleException extends MeasurementApplicationException {

    public HomeNotAccessibleException(String homeId) {
        super("home not found: " + homeId);
    }
}
