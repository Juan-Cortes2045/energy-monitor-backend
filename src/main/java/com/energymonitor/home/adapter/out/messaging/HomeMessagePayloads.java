package com.energymonitor.home.adapter.out.messaging;

import java.time.Instant;

/**
 * The JSON bodies of the messages in {@link HomeMessageTypes}, one record per type.
 *
 * <p>They are the wire contract, kept apart from the {@code api} records on purpose: a domain
 * event can gain a field without every consumer having to understand it. Instants travel as
 * ISO-8601 strings.
 */
public final class HomeMessagePayloads {

    /** {@code home.deleted} */
    public record HomeDeletedPayload(String homeId, String deletedBy, Instant deletedAt) {
    }

    private HomeMessagePayloads() {
    }
}
