package com.energymonitor.home.application.exception;

/**
 * Base for the application-layer exceptions. Nothing technology-specific lives here.
 */
public class HomeApplicationException extends RuntimeException {

    public HomeApplicationException(String message) {
        super(message);
    }

    public HomeApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
