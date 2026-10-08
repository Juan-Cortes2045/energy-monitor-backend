package com.energymonitor.recommendation.application.usecase;

import com.energymonitor.recommendation.api.RecommendationCreated;
import com.energymonitor.recommendation.application.port.in.EvaluateRecommendations;
import com.energymonitor.recommendation.application.port.out.ConsumptionReaderPort;
import com.energymonitor.recommendation.application.port.out.HomeReaderPort;
import com.energymonitor.recommendation.application.port.out.RecommendationEventPort;
import com.energymonitor.recommendation.application.port.out.RecommendationIdentifierPort;
import com.energymonitor.recommendation.application.port.out.RecommendationPersistencePort;
import com.energymonitor.recommendation.domain.model.MonitoredDevice;
import com.energymonitor.recommendation.domain.model.Recommendation;
import com.energymonitor.recommendation.domain.model.Suggestion;
import com.energymonitor.recommendation.domain.service.RecommendationRules;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates the rules for every home whose devices reported in the last week.
 *
 * <p>A suggestion already made for the same home, type and device within the cool-down (7 days by
 * default) is skipped, deleted ones included, so the same advice does not come back every hour.
 * Each new recommendation is published as {@link RecommendationCreated}. This service contains no
 * Spring annotations.
 */
public class EvaluateRecommendationsService implements EvaluateRecommendations {

    private static final Logger log = LoggerFactory.getLogger(EvaluateRecommendationsService.class);

    private final RecommendationPersistencePort recommendations;
    private final ConsumptionReaderPort consumption;
    private final HomeReaderPort homes;
    private final RecommendationEventPort events;
    private final RecommendationIdentifierPort identifiers;
    private final RecommendationRules rules;
    private final Clock clock;
    private final ZoneId zone;
    private final Duration coolDown;

    public EvaluateRecommendationsService(RecommendationPersistencePort recommendations,
                                          ConsumptionReaderPort consumption, HomeReaderPort homes,
                                          RecommendationEventPort events,
                                          RecommendationIdentifierPort identifiers,
                                          RecommendationRules rules, Clock clock, ZoneId zone,
                                          Duration coolDown) {
        this.recommendations = recommendations;
        this.consumption = consumption;
        this.homes = homes;
        this.events = events;
        this.identifiers = identifiers;
        this.rules = rules;
        this.clock = clock;
        this.zone = zone;
        this.coolDown = coolDown;
    }

    @Override
    public int evaluateAll() {
        Instant now = clock.instant();
        Set<String> homeIds = new LinkedHashSet<>();
        for (String deviceId : consumption.devicesReportingSince(now.minus(Duration.ofDays(7)))) {
            homes.homeOfDevice(deviceId).ifPresent(homeIds::add);
        }
        int created = 0;
        for (String homeId : homeIds) {
            try {
                created += evaluate(homeId, now);
            } catch (RuntimeException e) {
                log.warn("Recommendations of home {} could not be evaluated: {}", homeId, e.getMessage());
            }
        }
        return created;
    }

    @Override
    public int evaluateHome(String homeId) {
        return evaluate(homeId, clock.instant());
    }

    private int evaluate(String homeId, Instant now) {
        List<MonitoredDevice> devices = homes.devicesOf(homeId);
        if (devices.isEmpty()) {
            return 0;
        }
        var samples = consumption.hourly(devices.stream().map(MonitoredDevice::deviceId).toList(),
                RecommendationRules.dataFrom(now, zone), now);
        if (samples.isEmpty()) {
            return 0;
        }
        List<Suggestion> suggestions = rules.evaluate(now, zone, devices, samples, homes.limitOf(homeId));
        int created = 0;
        Instant dateTime = now.truncatedTo(ChronoUnit.SECONDS);
        for (Suggestion s : suggestions) {
            if (recommendations.existsSince(homeId, s.type(), s.deviceId(), now.minus(coolDown))) {
                continue;
            }
            Recommendation r = recommendations.save(Recommendation.create(identifiers.generate(), homeId,
                    s.deviceId(), s.type(), s.messageKey(), dateTime));
            events.publish(new RecommendationCreated(r.idRecommendation(), homeId, r.deviceId(), r.type(),
                    r.messageKey(), r.dateTime()));
            created++;
        }
        return created;
    }
}
