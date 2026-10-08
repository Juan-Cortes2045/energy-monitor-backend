package com.energymonitor.device.application.exception;

/**
 * Thrown when a device is not found.
 */
public class DeviceNotFoundException extends DeviceApplicationException {

    public DeviceNotFoundException(String deviceId) {
        super("device not found: " + deviceId);
    }
}
