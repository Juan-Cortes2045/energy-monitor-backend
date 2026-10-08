package com.energymonitor.recommendation.application.exception;

/**
 * The recommendation does not exist, was deleted, or the caller is not a member of its home.
 * Rendered as 404 so a non-member cannot probe identifiers.
 */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException(String message) {
        super(message);
    }
}
