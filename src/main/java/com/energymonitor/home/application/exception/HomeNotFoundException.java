package com.energymonitor.home.application.exception;

/**
 * Raised when an operation requires a home that does not exist, is soft-deleted,
 * or the user is not a member of.
 */
public class HomeNotFoundException extends HomeApplicationException {

    public HomeNotFoundException(String message) {
        super(message);
    }
}
