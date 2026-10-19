package com.energymonitor.home.application.exception;

/**
 * Raised when the user's identity cannot be resolved from the request.
 *
 * <p>This is a web-layer concern that maps to HTTP 401 Unauthorized.
 */
public class MissingUserIdentityException extends HomeApplicationException {

    public MissingUserIdentityException(String message) {
        super(message);
    }
}
