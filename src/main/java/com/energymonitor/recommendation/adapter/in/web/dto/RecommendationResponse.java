package com.energymonitor.recommendation.adapter.in.web.dto;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import java.time.Instant;

/**
 * Response DTO for a recommendation.
 *
 * @param deviceId   involved device, {@code null} when it is about the whole home
 * @param deviceName its name, so the client can render the message without another request
 * @param messageKey i18n message key
 */
public record RecommendationResponse(String idRecommendation, String homeId, String deviceId,
                                     String deviceName, RecommendationType type, String messageKey,
                                     Instant dateTime, RecommendationStatus status) {
}
