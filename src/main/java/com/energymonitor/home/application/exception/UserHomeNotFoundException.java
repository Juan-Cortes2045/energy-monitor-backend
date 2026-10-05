package com.energymonitor.home.application.exception;

/**
 * Raised when a membership does not exist or is soft-deleted.
 */
public class UserHomeNotFoundException extends HomeApplicationException {

    public UserHomeNotFoundException(String message) {
        super(message);
    }
}
