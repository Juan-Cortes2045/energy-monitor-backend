package com.energymonitor.home.api;

import java.time.Instant;

/**
 * A home was deleted: by its owner, or because the account of its only owner was deleted.
 *
 * <p>Published inside the transaction that deletes the home, and written to the home outbox in
 * that same transaction, so it is relayed to the broker as {@code home.deleted} only when the
 * deletion commits. That is how the device service hears it and unlinks the home's modules. A
 * synchronous listener in another module of this backend would commit or roll back with it too.
 *
 * @param homeId     the deleted home
 * @param deletedBy  the user who deleted it, or the deleted account
 * @param occurredAt when it was deleted
 */
public record HomeDeleted(String homeId, String deletedBy, Instant occurredAt) {
}
