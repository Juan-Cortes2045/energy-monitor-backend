package com.energymonitor.device.application.exception;

/**
 * Thrown when a home is not found.
 */
public class HomeNotFoundException extends DeviceApplicationException {

    public HomeNotFoundException(String homeId) {
        super("home not found: " + homeId);
    }
}
