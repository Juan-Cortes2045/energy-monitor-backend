package com.energymonitor.recommendation.adapter.in.scheduler;

import com.energymonitor.recommendation.application.port.in.EvaluateRecommendations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Evaluates the recommendation rules periodically ({@code recommendation.evaluate-interval}, one
 * hour by default). Disabled with {@code recommendation.enabled=false}, which the tests use.
 */
@Component
@ConditionalOnProperty(name = "recommendation.enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecommendationScheduler.class);

    private final EvaluateRecommendations evaluate;

    public RecommendationScheduler(EvaluateRecommendations evaluate) {
        this.evaluate = evaluate;
    }

    @Scheduled(initialDelayString = "${recommendation.initial-delay:PT2M}",
            fixedDelayString = "${recommendation.evaluate-interval:PT1H}")
    public void run() {
        try {
            int created = evaluate.evaluateAll();
            if (created > 0) {
                log.info("Generated {} recommendation(s)", created);
            }
        } catch (RuntimeException e) {
            log.warn("Recommendation evaluation failed: {}", e.getMessage());
        }
    }
}
