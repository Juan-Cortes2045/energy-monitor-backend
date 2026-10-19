package com.energymonitor.home.application.command;

/**
 * Input of {@code LeaveHome}: the user leaves a home.
 *
 * @param userId the leaving user
 * @param homeId the home to leave
 */
public record LeaveHomeCommand(String userId, String homeId) {
}
