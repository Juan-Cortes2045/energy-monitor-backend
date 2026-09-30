package com.energymonitor.home.application.exception;

/**
 * Raised when the user is a member of the home but lacks the required permission.
 */
public class HomeAccessDeniedException extends HomeApplicationException {

    public HomeAccessDeniedException(String message) {
        super(message);
    }
}
