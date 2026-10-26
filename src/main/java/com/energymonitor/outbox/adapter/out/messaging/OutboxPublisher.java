package com.energymonitor.outbox.adapter.out.messaging;

import java.time.Instant;

/**
 * Hands one outbox message to the broker and returns only once the broker has taken
 * responsibility for it.
 */
public interface OutboxPublisher {

    /**
     * @param idMessage  the message id, which consumers use to drop duplicates
     * @param exchange   the topic exchange
     * @param type       the routing key, also the message type
     * @param payload    the JSON body
     * @param occurredAt when the event was written
     * @throws RuntimeException when the broker did not confirm the message, or no queue took it
     */
    void publish(String idMessage, String exchange, String type, String payload, Instant occurredAt);
}
