package com.energymonitor.home.api;

import java.time.Instant;

/**
 * A home was deleted: by its owner, or because the account of its only owner was deleted.
 *
 * <p>Published inside the transaction that deletes the home, so a synchronous listener in another
 * module (the device module unlinks the home's modules) commits or rolls back together with it.
 * The home module cannot call those modules directly: it depends on nothing but
 * {@code security::api}, and the others depend on {@code home::api}.
 *
 * @param homeId     the deleted home
 * @param deletedBy  the user who deleted it, or the deleted account
 * @param occurredAt when it was deleted
 */
public record HomeDeleted(String homeId, String deletedBy, Instant occurredAt) {
}
