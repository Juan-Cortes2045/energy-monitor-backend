package com.energymonitor.outbox.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the outbox ({@code messaging.outbox.*}).
 *
 * @param relayEnabled   whether the relay runs. On by default: the notification service is the
 *                       only one that delivers the alerts. Off only to go back to a notification
 *                       service whose consumers are off, and in the test profile, so a test never
 *                       publishes to a broker another process is consuming
 * @param pollInterval   how often the relay looks for pending messages
 * @param batchSize      the most messages one run publishes
 * @param confirmTimeout how long to wait for the broker to confirm a message
 * @param discardWritten whether a message is stored already discarded; only the test profile
 *                       sets it, because the suite writes to the shared database and its
 *                       messages must never be published by a backend started later on it
 */
@ConfigurationProperties("messaging.outbox")
public record OutboxProperties(
        @DefaultValue("true") boolean relayEnabled,
        @DefaultValue("PT1S") Duration pollInterval,
        @DefaultValue("50") int batchSize,
        @DefaultValue("PT5S") Duration confirmTimeout,
        @DefaultValue("false") boolean discardWritten) {
}
