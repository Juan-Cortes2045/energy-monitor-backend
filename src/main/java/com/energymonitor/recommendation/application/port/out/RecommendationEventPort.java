package com.energymonitor.recommendation.application.port.out;

import com.energymonitor.recommendation.api.RecommendationCreated;

public interface RecommendationEventPort {

    void publish(RecommendationCreated event);
}
