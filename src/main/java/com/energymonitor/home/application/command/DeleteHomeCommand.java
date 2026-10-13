package com.energymonitor.home.application.command;

/**
 * @param userId the caller, who must be an OWNER of the home
 */
public record DeleteHomeCommand(String userId, String homeId) {
}
