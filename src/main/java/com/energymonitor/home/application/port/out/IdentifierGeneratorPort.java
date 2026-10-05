package com.energymonitor.home.application.port.out;

/**
 * Output port for generating the {@code VARCHAR(10)} business identifiers used across
 * the home aggregates.
 */
public interface IdentifierGeneratorPort {

    /**
     * Generates a new identifier fitting the {@code VARCHAR(10)} columns.
     *
     * @return a non-blank, at most 10 character identifier
     */
    String generate();
}
