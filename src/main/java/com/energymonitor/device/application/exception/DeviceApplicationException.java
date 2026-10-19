package com.energymonitor.device.application.exception;

/**
 * Base exception for device application layer.
 */
public abstract class DeviceApplicationException extends RuntimeException {

    protected DeviceApplicationException(String message) {
        super(message);
    }
}
