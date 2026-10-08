package com.energymonitor.recommendation.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.domain.model.Recommendation;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code recommendation} table. No fixtures: home and device are
 * application-level references without SQL foreign keys.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationPersistenceTest {

    private static final Instant T1 = Instant.parse("2026-01-15T08:00:00Z");
    private static final String HOME = "homRecT001";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecommendationPersistenceAdapter recommendations;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void roundTripAndMarkRead() {
        recommendations.save(Recommendation.create("recTest001", HOME, "devRecT001", RecommendationType.SAVING,
                "recommendation.standby", T1));
        flushAndClear();

        Recommendation read = recommendations.findActive("recTest001").orElseThrow();
        assertEquals(HOME, read.homeId());
        assertEquals("devRecT001", read.deviceId());
        assertEquals(RecommendationType.SAVING, read.type());
        assertEquals(T1, read.dateTime());
        assertEquals(RecommendationStatus.UNREAD, read.status());

        read.markRead();
        recommendations.save(read);
        flushAndClear();
        assertTrue(recommendations.findActive("recTest001").orElseThrow().isRead());
    }

    @Test
    void existsSinceMatchesDeviceOrWholeHomeAndIncludesDeleted() {
        recommendations.save(Recommendation.create("recTest002", HOME, null, RecommendationType.PEAK_HOURS,
                "recommendation.peakHours", T1));
        flushAndClear();
        recommendations.delete("recTest002");

        assertTrue(recommendations.existsSince(HOME, RecommendationType.PEAK_HOURS, null, T1));
        assertFalse(recommendations.existsSince(HOME, RecommendationType.PEAK_HOURS, "devRecT001", T1));
        assertFalse(recommendations.existsSince(HOME, RecommendationType.PEAK_HOURS, null, T1.plusSeconds(1)));
        assertTrue(recommendations.findActive("recTest002").isEmpty());
    }

    @Test
    void deleteReadOnlyRemovesReadOnes() {
        Recommendation a = Recommendation.create("recTest003", HOME, null, RecommendationType.THRESHOLD,
                "recommendation.limitProjection", T1);
        a.markRead();
        recommendations.save(a);
        recommendations.save(Recommendation.create("recTest004", HOME, null, RecommendationType.HISTORICAL_COMPARISON,
                "recommendation.aboveAverage", T1.plusSeconds(60)));
        flushAndClear();

        assertEquals(1, recommendations.deleteRead(HOME));
        assertEquals(1, recommendations.listActiveByHome(HOME).size());
    }
}
