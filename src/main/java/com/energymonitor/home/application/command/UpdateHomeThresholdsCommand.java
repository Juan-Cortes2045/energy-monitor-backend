package com.energymonitor.home.application.command;

/**
 * Input of {@code UpdateHomeThresholds}: updates the consumption thresholds of a home.
 *
 * @param userId       the requesting user (must have {@code canManageHome()})
 * @param homeId       the home to update
 * @param dailyLimit   new daily limit in kWh
 * @param monthlyLimit new monthly limit in kWh
 */
public record UpdateHomeThresholdsCommand(String userId, String homeId, double dailyLimit, double monthlyLimit) {
}
