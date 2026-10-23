package com.energymonitor.home.adapter.out.messaging;

import com.energymonitor.home.adapter.out.persistence.entity.HomeOutboxEntity;
import com.energymonitor.home.adapter.out.persistence.repository.HomeOutboxRepository;
import com.energymonitor.home.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.home.infrastructure.HomeOutboxProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a home message to the outbox.
 *
 * <p>Called from inside the use case's transaction, so the row is committed or rolled back
 * together with the change it announces. Publishing to the broker is left to
 * {@link HomeOutboxRelay}.
 */
@Component
public class HomeOutboxWriter {

    private final HomeOutboxRepository outbox;
    private final IdentifierGeneratorPort identifiers;
    private final ObjectMapper json;
    private final Clock clock;
    private final boolean discardWritten;

    public HomeOutboxWriter(HomeOutboxRepository outbox, IdentifierGeneratorPort identifiers,
                            ObjectMapper json, Clock clock, HomeOutboxProperties properties) {
        this.outbox = outbox;
        this.identifiers = identifiers;
        this.json = json;
        this.clock = clock;
        this.discardWritten = properties.discardWritten();
    }

    /**
     * @param type       the routing key, one of {@link HomeMessageTypes}
     * @param payload    the body, serialized as JSON
     * @param occurredAt when the change happened
     */
    public void write(String type, Object payload, Instant occurredAt) {
        HomeOutboxEntity message = new HomeOutboxEntity();
        message.setIdMessage(identifiers.generate());
        message.setMessageType(type);
        message.setPayload(json.writeValueAsString(payload));
        message.setOccurredAt(occurredAt);
        if (discardWritten) {
            message.setDiscardedAt(clock.instant());
        }
        outbox.save(message);
    }
}
