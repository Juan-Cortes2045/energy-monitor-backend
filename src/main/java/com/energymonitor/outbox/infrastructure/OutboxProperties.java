package com.energymonitor.outbox.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the outbox ({@code messaging.outbox.*}).
 *
 * @param relayEnabled   whether the relay runs. Off by default while this backend still has its
 *                       notification module, which delivers the alerts itself; on in the
 *                       deployment that removes it. Off in the test profile too, so a test never
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
        @DefaultValue("false") boolean relayEnabled,
        @DefaultValue("PT1S") Duration pollInterval,
        @DefaultValue("50") int batchSize,
        @DefaultValue("PT5S") Duration confirmTimeout,
        @DefaultValue("false") boolean discardWritten) {
}
