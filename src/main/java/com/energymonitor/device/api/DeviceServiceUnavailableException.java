package com.energymonitor.device.api;

/**
 * The device service did not answer in time, or answered with an error, and no earlier answer
 * was kept to fall back on. A recommendation waits for the next run.
 */
public class DeviceServiceUnavailableException extends RuntimeException {

    public DeviceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
