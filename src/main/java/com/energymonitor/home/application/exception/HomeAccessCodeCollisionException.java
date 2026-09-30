package com.energymonitor.home.application.exception;

/**
 * Raised when an access code collision cannot be resolved after retries.
 *
 * <p>This is a domain-level signal translated by the persistence adapter when a unique
 * constraint violation occurs on {@code access_code}.
 */
public class HomeAccessCodeCollisionException extends HomeApplicationException {

    public HomeAccessCodeCollisionException(String message) {
        super(message);
    }

    public HomeAccessCodeCollisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
