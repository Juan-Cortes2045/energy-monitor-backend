package com.energymonitor.home.application.command;

/**
 * Input of {@code ToggleFavorite}: toggles the favorite flag of a home for a user.
 *
 * @param userId the user
 * @param homeId the home to toggle
 */
public record ToggleFavoriteCommand(String userId, String homeId) {
}
