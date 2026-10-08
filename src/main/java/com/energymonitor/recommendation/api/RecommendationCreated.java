package com.energymonitor.recommendation.api;

import java.time.Instant;

/**
 * Application event published after a recommendation is persisted.
 *
 * <p>The {@code notification} module delivers it to the members of the home; the payload carries
 * everything it needs to build the message.
 *
 * @param idRecommendation identifier of the new recommendation
 * @param homeId           home it belongs to
 * @param deviceId         involved device, {@code null} when it is about the whole home
 * @param type             what the recommendation is about
 * @param messageKey       i18n message key
 * @param dateTime         when it was generated
 */
public record RecommendationCreated(String idRecommendation, String homeId, String deviceId,
                                    RecommendationType type, String messageKey, Instant dateTime) {
}
