package com.energymonitor.home.api;

/**
 * The home service did not answer in time, or answered with an error, and no earlier answer was
 * kept to fall back on. Each caller degrades in its own way: an alert waits for the next reading,
 * a notification is sent later.
 */
public class HomeServiceUnavailableException extends RuntimeException {

    public HomeServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
