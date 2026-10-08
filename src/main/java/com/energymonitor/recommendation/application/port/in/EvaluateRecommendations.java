package com.energymonitor.recommendation.application.port.in;

/**
 * Runs the rules over every home with recent readings and stores what they suggest.
 */
public interface EvaluateRecommendations {

    /** @return how many recommendations were created */
    int evaluateAll();

    /** @return how many recommendations were created for this home */
    int evaluateHome(String homeId);
}
