package com.energymonitor.recommendation.adapter.out.event;

import com.energymonitor.outbox.api.OutboxWriter;
import com.energymonitor.recommendation.api.RecommendationCreated;
import com.energymonitor.recommendation.application.port.out.RecommendationEventPort;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Announces a new recommendation as {@code recommendation.created} on the topic exchange
 * {@code messaging.recommendation-exchange}, through the outbox, for the notification service,
 * which delivers it to the members of the home.
 *
 * <p>The body of {@code recommendation.created} is the contract with the notification service:
 * {@code {idRecommendation, homeId, deviceId, type, messageKey, dateTime}}.
 */
@Component
public class RecommendationEventAdapter implements RecommendationEventPort {

    static final String RECOMMENDATION_CREATED = "recommendation.created";

    private final OutboxWriter outbox;
    private final String exchange;

    public RecommendationEventAdapter(OutboxWriter outbox,
                                      @Value("${messaging.recommendation-exchange}") String exchange) {
        this.outbox = outbox;
        this.exchange = exchange;
    }

    @Override
    public void publish(RecommendationCreated event) {
        outbox.write(exchange, RECOMMENDATION_CREATED, new RecommendationCreatedMessage(
                event.idRecommendation(), event.homeId(), event.deviceId(), event.type().name(),
                event.messageKey(), event.dateTime()));
    }

    /**
     * Body of {@code recommendation.created}.
     *
     * @param deviceId null when the recommendation is about the whole home
     */
    record RecommendationCreatedMessage(String idRecommendation, String homeId, String deviceId,
                                        String type, String messageKey, Instant dateTime) {
    }
}
