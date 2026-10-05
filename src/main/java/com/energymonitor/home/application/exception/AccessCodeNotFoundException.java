package com.energymonitor.home.application.exception;

/**
 * Raised when an access code does not match any active home.
 */
public class AccessCodeNotFoundException extends HomeApplicationException {

    public AccessCodeNotFoundException(String message) {
        super(message);
    }
}
