package com.energymonitor.home.application.command;

/**
 * Input of {@code JoinHome}: joins a home with an access code.
 *
 * @param userId     the joining user (becomes MEMBER)
 * @param accessCode the 8-character join code
 */
public record JoinHomeCommand(String userId, String accessCode) {
}
