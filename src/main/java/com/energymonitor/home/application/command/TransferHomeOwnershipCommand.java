package com.energymonitor.home.application.command;

/**
 * Input of {@code TransferHomeOwnership}.
 *
 * @param userId     the caller, who must be the OWNER of the home
 * @param homeId     the home to hand over
 * @param newOwnerId the active member who becomes the owner
 */
public record TransferHomeOwnershipCommand(String userId, String homeId, String newOwnerId) {
}
