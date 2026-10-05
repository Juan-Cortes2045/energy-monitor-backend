package com.energymonitor.home.application.command;

/**
 * Input of {@code GetHomeThresholds}: retrieves the thresholds of a home.
 *
 * @param userId the requesting user (must be a member)
 * @param homeId the home whose thresholds are read
 */
public record GetHomeThresholdsQuery(String userId, String homeId) {
}
