package com.energymonitor.recommendation.infrastructure;

import com.energymonitor.recommendation.application.port.out.ConsumptionReaderPort;
import com.energymonitor.recommendation.application.port.out.HomeReaderPort;
import com.energymonitor.recommendation.application.port.out.RecommendationEventPort;
import com.energymonitor.recommendation.application.port.out.RecommendationIdentifierPort;
import com.energymonitor.recommendation.application.port.out.RecommendationPersistencePort;
import com.energymonitor.recommendation.application.usecase.EvaluateRecommendationsService;
import com.energymonitor.recommendation.application.usecase.RecommendationInboxService;
import com.energymonitor.recommendation.domain.service.RecommendationRules;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the framework-free use cases of the Recommendations module.
 *
 * <p>{@code recommendation.zone} is the zone the day-part rules use (evening peak, night
 * standby, calendar month); {@code recommendation.cool-down} is how long the same advice is not
 * repeated for the same home and device.
 */
@Configuration
@EnableScheduling
public class RecommendationServiceConfiguration {

    @Bean
    RecommendationInboxService recommendationInboxService(RecommendationPersistencePort recommendations,
                                                          HomeReaderPort homes) {
        return new RecommendationInboxService(recommendations, homes);
    }

    @Bean
    EvaluateRecommendationsService evaluateRecommendationsService(
            RecommendationPersistencePort recommendations, ConsumptionReaderPort consumption,
            HomeReaderPort homes, RecommendationEventPort events, RecommendationIdentifierPort identifiers,
            @Value("${recommendation.zone:America/Bogota}") String zone,
            @Value("${recommendation.cool-down:P7D}") Duration coolDown) {
        return new EvaluateRecommendationsService(recommendations, consumption, homes, events, identifiers,
                new RecommendationRules(), Clock.systemUTC(), ZoneId.of(zone), coolDown);
    }
}
