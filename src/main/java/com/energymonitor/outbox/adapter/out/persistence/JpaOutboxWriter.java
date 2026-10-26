package com.energymonitor.outbox.adapter.out.persistence;

import com.energymonitor.outbox.adapter.out.persistence.entity.OutboxMessageEntity;
import com.energymonitor.outbox.adapter.out.persistence.repository.OutboxMessageRepository;
import com.energymonitor.outbox.api.OutboxWriter;
import java.security.SecureRandom;
import java.time.Clock;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes an event to {@code backend_outbox}, for the relay to publish.
 *
 * <p>The message id is generated here, {@code VARCHAR(10)} from
 * {@code ABCDEFGHJKMNPQRSTUVWXYZ23456789} like the other identifiers; it is also the message id
 * on the broker. The primary key is the final guard against a collision.
 */
public class JpaOutboxWriter implements OutboxWriter {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 10;

    private final OutboxMessageRepository outbox;
    private final ObjectMapper json;
    private final Clock clock;
    private final boolean discardWritten;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param discardWritten whether every message is stored already discarded, so it is never
     *                       published; only the test profile sets it
     */
    public JpaOutboxWriter(OutboxMessageRepository outbox, ObjectMapper json, Clock clock,
                           boolean discardWritten) {
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
        this.discardWritten = discardWritten;
    }

    @Override
    public void write(String exchange, String type, Object payload) {
        OutboxMessageEntity message = new OutboxMessageEntity();
        message.setIdMessage(generateId());
        message.setExchange(exchange);
        message.setMessageType(type);
        message.setPayload(json.writeValueAsString(payload));
        message.setOccurredAt(clock.instant());
        if (discardWritten) {
            message.setDiscardedAt(clock.instant());
        }
        outbox.save(message);
    }

    private String generateId() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
