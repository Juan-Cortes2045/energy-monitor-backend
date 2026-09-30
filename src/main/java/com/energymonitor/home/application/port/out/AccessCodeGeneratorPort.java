package com.energymonitor.home.application.port.out;

/**
 * Output port for generating the 8-character access codes used to join homes.
 */
public interface AccessCodeGeneratorPort {

    /**
     * Generates a new access code of exactly 8 characters.
     *
     * @return a non-blank, 8 character code
     */
    String generate();
}
