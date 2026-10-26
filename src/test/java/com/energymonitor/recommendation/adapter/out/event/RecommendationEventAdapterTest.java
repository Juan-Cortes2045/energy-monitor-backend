package com.energymonitor.recommendation.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.energymonitor.outbox.api.OutboxWriter;
import com.energymonitor.recommendation.adapter.out.event.RecommendationEventAdapter.RecommendationCreatedMessage;
import com.energymonitor.recommendation.api.RecommendationCreated;
import com.energymonitor.recommendation.api.RecommendationType;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("Recommendation events")
class RecommendationEventAdapterTest {

    private static final Instant AT = Instant.parse("2026-10-23T15:00:00Z");

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final RecommendationEventAdapter adapter =
            new RecommendationEventAdapter(publisher, outbox, "energy-monitor.recommendation");

    @Test
    @DisplayName("a recommendation goes to the outbox as recommendation.created, and in process")
    void writesRecommendationCreated() {
        RecommendationCreated event = new RecommendationCreated("rec0000001", "hom0000001", null,
                RecommendationType.PEAK_HOURS, "recommendation.peakHours", AT);

        adapter.publish(event);

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(outbox).write(eq("energy-monitor.recommendation"), eq("recommendation.created"),
                body.capture());
        assertThat(body.getValue()).isEqualTo(new RecommendationCreatedMessage("rec0000001",
                "hom0000001", null, "PEAK_HOURS", "recommendation.peakHours", AT));
        verify(publisher).publishEvent(event);
    }
}
