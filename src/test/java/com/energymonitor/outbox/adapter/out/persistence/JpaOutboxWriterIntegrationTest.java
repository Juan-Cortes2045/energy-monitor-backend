package com.energymonitor.outbox.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.energymonitor.outbox.adapter.out.persistence.entity.OutboxMessageEntity;
import com.energymonitor.outbox.adapter.out.persistence.repository.OutboxMessageRepository;
import com.energymonitor.outbox.api.OutboxWriter;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * An event written to the outbox, against the real schema. {@code @Transactional}: the test's
 * transaction is rolled back, so no row is left behind; the test profile also stores the rows
 * already discarded.
 */
@SpringBootTest
@Transactional
@DisplayName("Outbox writer")
class JpaOutboxWriterIntegrationTest extends JwtKeyedTest {

    private static final String HOME = "hob0000009";

    @Autowired
    private OutboxWriter writer;
    @Autowired
    private OutboxMessageRepository outbox;
    @Autowired
    private ObjectMapper json;

    @Test
    @DisplayName("stores the exchange, the type and the body as JSON, pending for the relay")
    void storesTheEvent() {
        writer.write("energy-monitor.alert", "alert.raised",
                new Body("alr0000009", HOME, Instant.parse("2026-10-23T15:00:00Z")));

        List<OutboxMessageEntity> messages = messagesFor(HOME);
        assertThat(messages).hasSize(1);
        OutboxMessageEntity message = messages.getFirst();
        assertThat(message.getIdMessage()).hasSize(10);
        assertThat(message.getExchange()).isEqualTo("energy-monitor.alert");
        assertThat(message.getMessageType()).isEqualTo("alert.raised");
        assertThat(message.getOccurredAt()).isNotNull();
        assertThat(message.getAttempts()).isZero();
        JsonNode payload = json.readTree(message.getPayload());
        assertThat(payload.get("idAlert").asString()).isEqualTo("alr0000009");
        assertThat(payload.get("dateTime").asString()).isEqualTo("2026-10-23T15:00:00Z");
        assertThat(message.getPublishedAt()).isNull();
        assertThat(message.getDiscardedAt()).as("the test profile never lets it be published").isNotNull();
    }

    private List<OutboxMessageEntity> messagesFor(String homeId) {
        return outbox.findAll().stream()
                .filter(message -> message.getPayload().contains("\"" + homeId + "\""))
                .toList();
    }

    record Body(String idAlert, String homeId, Instant dateTime) {
    }
}
