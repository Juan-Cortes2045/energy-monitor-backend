package com.energymonitor.home.application.command;

/**
 * Input of {@code RemoveUser}: removes a user from a home.
 *
 * @param userId        the requesting user (must have {@code canManageHome()})
 * @param homeId        the home from which the user is removed
 * @param targetUserId  the user to remove
 */
public record RemoveUserCommand(String userId, String homeId, String targetUserId) {
}
