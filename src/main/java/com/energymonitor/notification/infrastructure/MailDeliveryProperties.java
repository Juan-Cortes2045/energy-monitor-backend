package com.energymonitor.notification.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Recovery settings for delivery records, bound from {@code app.mail.*}.
 *
 * <p>Queue size and thread count are not here: the queue is declared by the security module and
 * sized under {@code security.reset.executor.*}. What remains describes the two values that are
 * about a row rather than about a thread: how long a row may claim a delivery that is no longer in
 * flight, and how often that claim is checked.
 *
 * @param pendingThreshold how long a row may stay {@code PENDING} before the sweep closes it as
 *                         failed, which bounds how long a row can claim a send that never happened
 * @param sweepDelay       how often the sweep runs
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailDeliveryProperties(
        Duration pendingThreshold,
        Duration sweepDelay) {

    /**
     * How long a {@code PENDING} row may claim a send that is no longer in flight.
     *
     * <p>Generous compared with any real SMTP conversation, because the alternative is closing rows
     * whose send is merely slow, and short compared with the age of a record nobody will ever look
     * at again.
     */
    public static final Duration DEFAULT_PENDING_THRESHOLD = Duration.ofMinutes(30);

    /** How often the sweep runs. Rare: it only acts on rows abandoned by a previous run. */
    public static final Duration DEFAULT_SWEEP_DELAY = Duration.ofMinutes(5);
}