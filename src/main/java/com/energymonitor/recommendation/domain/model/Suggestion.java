package com.energymonitor.recommendation.domain.model;

import com.energymonitor.recommendation.api.RecommendationType;

/**
 * What a rule concluded, before it becomes a {@link Recommendation}.
 *
 * @param deviceId {@code null} when it is about the whole home
 */
public record Suggestion(RecommendationType type, String deviceId, String messageKey) {
}
