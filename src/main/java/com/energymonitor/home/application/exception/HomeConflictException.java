package com.energymonitor.home.application.exception;

/**
 * Raised when an operation conflicts with the current state of the home
 * (e.g. duplicate membership, access code collision, removing an OWNER).
 */
public class HomeConflictException extends HomeApplicationException {

    public HomeConflictException(String message) {
        super(message);
    }
}
