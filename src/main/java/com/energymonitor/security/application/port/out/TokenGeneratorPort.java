package com.energymonitor.security.application.port.out;

/**
 * Output port for generating the opaque random tokens the security aggregates store: session
 * refresh tokens and password-reset tokens. Nothing about signing or encoding them belongs to
 * the application layer.
 */
public interface TokenGeneratorPort {

    /**
     * Generates a fresh opaque token.
     *
     * @return a non-blank random token value
     */
    String generateToken();
}