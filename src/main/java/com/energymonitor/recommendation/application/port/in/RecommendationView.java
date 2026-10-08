package com.energymonitor.recommendation.application.port.in;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import java.time.Instant;

/**
 * A recommendation as the inbox shows it.
 *
 * @param deviceName name of the device, {@code null} when home-scoped or the device is gone
 */
public record RecommendationView(String idRecommendation, String homeId, String deviceId,
                                 String deviceName, RecommendationType type, String messageKey,
                                 Instant dateTime, RecommendationStatus status) {
}
