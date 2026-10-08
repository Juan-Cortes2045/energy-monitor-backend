package com.energymonitor.recommendation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.application.usecase.EvaluateRecommendationsService;
import com.energymonitor.recommendation.domain.model.HourSample;
import com.energymonitor.recommendation.domain.service.RecommendationRules;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluateRecommendationsServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final Instant NOW = Instant.parse("2026-05-20T17:00:00Z");

    private final RecommendationFakes.Store store = new RecommendationFakes.Store();
    private final RecommendationFakes.Homes homes = new RecommendationFakes.Homes();
    private final RecommendationFakes.Consumption consumption = new RecommendationFakes.Consumption();
    private final RecommendationFakes.Events events = new RecommendationFakes.Events();
    private final AtomicInteger ids = new AtomicInteger();

    private EvaluateRecommendationsService service(Instant now) {
        return new EvaluateRecommendationsService(store, consumption, homes, events,
                () -> "rec%07d".formatted(ids.incrementAndGet()), new RecommendationRules(),
                Clock.fixed(now, ZoneOffset.UTC), ZONE, Duration.ofDays(7));
    }

    @BeforeEach
    void standbyDevice() {
        homes.device("hom0000001", "dev0000001", false);
        for (Instant t = NOW.minus(Duration.ofDays(7)); t.isBefore(NOW); t = t.plus(Duration.ofHours(1))) {
            consumption.samples.add(new HourSample("dev0000001", t, 30, 0.03));
        }
    }

    @Test
    void createsUnreadRecommendationAndPublishesIt() {
        assertEquals(1, service(NOW).evaluateAll());

        var created = store.listActiveByHome("hom0000001");
        assertEquals(1, created.size());
        assertEquals(RecommendationType.SAVING, created.get(0).type());
        assertEquals("dev0000001", created.get(0).deviceId());
        assertEquals(RecommendationStatus.UNREAD, created.get(0).status());
        assertEquals(1, events.published.size());
        assertEquals(created.get(0).idRecommendation(), events.published.get(0).idRecommendation());
    }

    @Test
    void doesNotRepeatWithinTheCoolDownEvenWhenDeleted() {
        service(NOW).evaluateAll();
        store.delete(store.listActiveByHome("hom0000001").get(0).idRecommendation());

        assertEquals(0, service(NOW.plus(Duration.ofHours(1))).evaluateAll());
        assertEquals(1, events.published.size());
    }

    @Test
    void repeatsAfterTheCoolDown() {
        service(NOW).evaluateAll();
        Instant later = NOW.plus(Duration.ofDays(8));
        for (Instant t = NOW; t.isBefore(later); t = t.plus(Duration.ofHours(1))) {
            consumption.samples.add(new HourSample("dev0000001", t, 30, 0.03));
        }
        assertEquals(1, service(later).evaluateAll());
    }

    @Test
    void homesWithoutRecentReadingsAreSkipped() {
        assertEquals(0, service(NOW.plus(Duration.ofDays(30))).evaluateAll());
    }
}
