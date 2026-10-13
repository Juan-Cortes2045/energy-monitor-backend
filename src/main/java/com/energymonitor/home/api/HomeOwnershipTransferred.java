package com.energymonitor.home.api;

import java.time.Instant;

/**
 * The owner of a home handed it over to one of its members.
 *
 * <p>Published inside the transaction that swaps the two roles, so a synchronous listener in
 * another module commits or rolls back together with the transfer. No module reacts to it yet;
 * it is here so one can (for example, notifications telling the new owner) without the home module
 * having to know about it.
 *
 * @param homeId          the home
 * @param previousOwnerId the user who was the owner and is now a member
 * @param newOwnerId      the member who is now the owner
 * @param occurredAt      when the transfer happened
 */
public record HomeOwnershipTransferred(String homeId, String previousOwnerId, String newOwnerId,
                                       Instant occurredAt) {
}
