package com.energymonitor.home.application.command;

/**
 * Input of {@code CreateHome}: creates a new home with the user as OWNER.
 *
 * @param userId      the creating user (becomes OWNER)
 * @param name        display name, {@code VARCHAR(50)}
 * @param homeTypeId  identifier of the home type
 * @param address     physical address, {@code VARCHAR(200)}
 * @param description optional description, {@code VARCHAR(200)}
 */
public record CreateHomeCommand(String userId, String name, String homeTypeId, String address, String description) {
}
