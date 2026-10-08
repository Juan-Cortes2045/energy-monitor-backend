package com.energymonitor.recommendation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.application.exception.RecommendationNotFoundException;
import com.energymonitor.recommendation.application.usecase.RecommendationInboxService;
import com.energymonitor.recommendation.domain.model.Recommendation;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationInboxServiceTest {

    private final RecommendationFakes.Store store = new RecommendationFakes.Store();
    private final RecommendationFakes.Homes homes = new RecommendationFakes.Homes();
    private final RecommendationInboxService inbox = new RecommendationInboxService(store, homes);

    @BeforeEach
    void seed() {
        homes.members.put("hom0000001", Set.of("usr0000001"));
        store.save(Recommendation.create("rec0000001", "hom0000001", "dev0000001", RecommendationType.SAVING,
                "recommendation.standby", Instant.parse("2026-05-20T10:00:00Z")));
        store.save(Recommendation.create("rec0000002", "hom0000001", null, RecommendationType.PEAK_HOURS,
                "recommendation.peakHours", Instant.parse("2026-05-20T11:00:00Z")));
    }

    @Test
    void listsNewestFirstWithDeviceNames() {
        var list = inbox.list("usr0000001", "hom0000001");
        assertEquals("rec0000002", list.get(0).idRecommendation());
        assertEquals(null, list.get(0).deviceName());
        assertEquals("Device dev0000001", list.get(1).deviceName());
    }

    @Test
    void nonMembersSeeNothing() {
        assertThrows(RecommendationNotFoundException.class, () -> inbox.list("intruder01", "hom0000001"));
        assertThrows(RecommendationNotFoundException.class, () -> inbox.markRead("intruder01", "rec0000001"));
        assertThrows(RecommendationNotFoundException.class, () -> inbox.delete("intruder01", "rec0000001"));
    }

    @Test
    void onlyReadRecommendationsCanBeDeleted() {
        assertThrows(IllegalStateException.class, () -> inbox.delete("usr0000001", "rec0000001"));

        assertEquals(RecommendationStatus.READ, inbox.markRead("usr0000001", "rec0000001").status());
        assertEquals(RecommendationStatus.READ, inbox.markRead("usr0000001", "rec0000001").status());
        inbox.delete("usr0000001", "rec0000001");
        assertEquals(1, inbox.list("usr0000001", "hom0000001").size());
    }

    @Test
    void deleteReadLeavesUnreadOnes() {
        inbox.markRead("usr0000001", "rec0000002");
        assertEquals(1, inbox.deleteRead("usr0000001", "hom0000001"));
        assertEquals("rec0000001", inbox.list("usr0000001", "hom0000001").get(0).idRecommendation());
    }
}
