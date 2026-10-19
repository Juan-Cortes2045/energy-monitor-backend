package com.energymonitor.home.application.exception;

/**
 * Raised when a home type does not exist or is soft-deleted.
 */
public class HomeTypeNotFoundException extends HomeApplicationException {

    public HomeTypeNotFoundException(String message) {
        super(message);
    }
}
