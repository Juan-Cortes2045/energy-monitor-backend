package com.energymonitor.recommendation.application.port.out;

/** Generates {@code VARCHAR(10)} identifiers. */
public interface RecommendationIdentifierPort {

    String generate();
}
