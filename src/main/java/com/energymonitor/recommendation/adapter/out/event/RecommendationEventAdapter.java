package com.energymonitor.recommendation.adapter.out.event;

import com.energymonitor.recommendation.api.RecommendationCreated;
import com.energymonitor.recommendation.application.port.out.RecommendationEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes recommendation events to the Spring application context.
 */
@Component
public class RecommendationEventAdapter implements RecommendationEventPort {

    private final ApplicationEventPublisher publisher;

    public RecommendationEventAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(RecommendationCreated event) {
        publisher.publishEvent(event);
    }
}
