package com.energymonitor.outbox.adapter.out.messaging;

import com.energymonitor.outbox.adapter.out.persistence.entity.OutboxMessageEntity;
import com.energymonitor.outbox.adapter.out.persistence.repository.OutboxMessageRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

/**
 * Moves the outbox to the broker.
 *
 * <p>Each run takes the pending messages oldest first and publishes them one by one, waiting for
 * the broker's confirmation before marking a message as published. The first failure stops the
 * run, so a later message never overtakes an earlier one; the failed message is retried on the
 * next run, which is what makes delivery at-least-once. The notification service drops a
 * duplicate when its delivery log already holds the alert or recommendation.
 *
 * <p>Runs on a single instance of the backend. A second instance would publish the same rows
 * twice, which the consumer tolerates, but it would also break the ordering above.
 */
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private static final int MAX_ERROR_LENGTH = 255;

    private final OutboxMessageRepository outbox;
    private final OutboxPublisher publisher;
    private final Clock clock;
    private final int batchSize;

    public OutboxRelay(OutboxMessageRepository outbox, OutboxPublisher publisher, Clock clock,
                       int batchSize) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    /**
     * Publishes what is pending.
     *
     * @return how many messages were published in this run
     */
    public int relay() {
        int published = 0;
        for (OutboxMessageEntity message : outbox.findPending(PageRequest.of(0, batchSize))) {
            try {
                publisher.publish(message.getIdMessage(), message.getExchange(),
                        message.getMessageType(), message.getPayload(), message.getOccurredAt());
            } catch (RuntimeException failure) {
                message.setAttempts(message.getAttempts() + 1);
                message.setLastError(truncate(failure.getMessage()));
                outbox.save(message);
                LOG.warn("Outbox message {} ({}) not published, attempt {}: {}",
                        message.getIdMessage(), message.getMessageType(), message.getAttempts(),
                        failure.getMessage());
                break;
            }
            message.setPublishedAt(clock.instant());
            outbox.save(message);
            published++;
        }
        return published;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
