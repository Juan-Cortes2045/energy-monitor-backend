package com.energymonitor.outbox.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.energymonitor.outbox.adapter.out.persistence.entity.OutboxMessageEntity;
import com.energymonitor.outbox.adapter.out.persistence.repository.OutboxMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@DisplayName("Outbox relay")
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-23T15:00:00Z");

    private final OutboxMessageRepository outbox = mock(OutboxMessageRepository.class);
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final OutboxRelay relay =
            new OutboxRelay(outbox, publisher, Clock.fixed(NOW, ZoneOffset.UTC), 50);

    @Test
    @DisplayName("publishes the pending messages oldest first, each to its exchange, and marks them")
    void publishesInOrder() {
        var alert = message("obx0000001", "energy-monitor.alert", "alert.raised");
        var recommendation =
                message("obx0000002", "energy-monitor.recommendation", "recommendation.created");
        given(outbox.findPending(any(Pageable.class))).willReturn(List.of(alert, recommendation));

        int published = relay.relay();

        assertThat(published).isEqualTo(2);
        assertThat(publisher.sent).containsExactly(
                "obx0000001 energy-monitor.alert alert.raised",
                "obx0000002 energy-monitor.recommendation recommendation.created");
        assertThat(alert.getPublishedAt()).isEqualTo(NOW);
        assertThat(recommendation.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("stops at the first failure, so a later message never overtakes it")
    void stopsAtFirstFailure() {
        var failing = message("obx0000003", "energy-monitor.alert", "alert.raised");
        var later = message("obx0000004", "energy-monitor.alert", "alert.raised");
        given(outbox.findPending(any(Pageable.class))).willReturn(List.of(failing, later));
        publisher.failOn = "obx0000003";

        int published = relay.relay();

        assertThat(published).isZero();
        assertThat(publisher.sent).hasSize(1);
        assertThat(failing.getPublishedAt()).isNull();
        assertThat(failing.getAttempts()).isEqualTo(1);
        assertThat(failing.getLastError()).isEqualTo("no queue is bound for alert.raised yet");
        assertThat(later.getPublishedAt()).isNull();
        assertThat(later.getAttempts()).isZero();
    }

    @Test
    @DisplayName("a long error is cut to the column size")
    void truncatesLongErrors() {
        var failing = message("obx0000005", "energy-monitor.alert", "alert.raised");
        given(outbox.findPending(any(Pageable.class))).willReturn(List.of(failing));
        publisher.failOn = "obx0000005";
        publisher.error = "x".repeat(400);

        relay.relay();

        assertThat(failing.getLastError()).hasSize(255);
    }

    @Test
    @DisplayName("nothing pending publishes nothing")
    void nothingPending() {
        given(outbox.findPending(any(Pageable.class))).willReturn(List.of());

        assertThat(relay.relay()).isZero();
        assertThat(publisher.sent).isEmpty();
    }

    private static OutboxMessageEntity message(String id, String exchange, String type) {
        var message = new OutboxMessageEntity();
        message.setIdMessage(id);
        message.setExchange(exchange);
        message.setMessageType(type);
        message.setPayload("{\"homeId\":\"hom0000001\"}");
        message.setOccurredAt(NOW.minusSeconds(5));
        return message;
    }

    private static final class RecordingPublisher implements OutboxPublisher {

        private final List<String> sent = new ArrayList<>();
        private String failOn;
        private String error = "no queue is bound for alert.raised yet";

        @Override
        public void publish(String idMessage, String exchange, String type, String payload,
                            Instant occurredAt) {
            sent.add(idMessage + " " + exchange + " " + type);
            if (idMessage.equals(failOn)) {
                throw new IllegalStateException(error);
            }
        }
    }
}
