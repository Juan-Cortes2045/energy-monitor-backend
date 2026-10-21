package com.energymonitor.security.api;

/**
 * The security service did not answer, or answered with an error. Each caller decides how to
 * degrade: the member list shows no names, an administration route answers 503, and a message
 * consumer lets the broker retry.
 */
public class SecurityServiceUnavailableException extends RuntimeException {

    public SecurityServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
