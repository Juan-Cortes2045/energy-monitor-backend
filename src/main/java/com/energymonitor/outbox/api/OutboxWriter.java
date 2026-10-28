package com.energymonitor.outbox.api;

/**
 * Stores an event to be published to the message broker.
 *
 * <p>Nothing is sent here: the event is written to the outbox, and the relay publishes it later.
 * Called from a transaction, the event is committed or rolled back with it.
 */
public interface OutboxWriter {

    /**
     * @param exchange the topic exchange the event goes to, such as {@code energy-monitor.alert}
     * @param type     the routing key, also the message type, such as {@code alert.raised}
     * @param payload  the body, serialized as JSON
     */
    void write(String exchange, String type, Object payload);
}
