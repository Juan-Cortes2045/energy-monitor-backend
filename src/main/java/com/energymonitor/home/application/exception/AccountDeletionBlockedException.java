package com.energymonitor.home.application.exception;

/**
 * Refuses the deletion of an account that is the only owner of a home still having other
 * members. The message reaches the user through the security service, so it must be readable.
 */
public class AccountDeletionBlockedException extends HomeApplicationException {

    public AccountDeletionBlockedException(String message) {
        super(message);
    }
}
