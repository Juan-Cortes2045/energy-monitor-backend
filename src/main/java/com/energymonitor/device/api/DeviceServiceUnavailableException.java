package com.energymonitor.device.api;

/**
 * The device service did not answer in time, or answered with an error, and no earlier answer
 * was kept to fall back on. Each caller degrades in its own way: a reading of a device nobody can
 * confirm is not stored, an alert waits for the next one.
 */
public class DeviceServiceUnavailableException extends RuntimeException {

    public DeviceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
