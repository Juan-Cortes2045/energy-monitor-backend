package com.energymonitor.home.application.exception;

/**
 * Raised when the only OWNER attempts to leave the home.
 */
public class LastOwnerCannotLeaveException extends HomeConflictException {

    public LastOwnerCannotLeaveException(String message) {
        super(message);
    }
}
