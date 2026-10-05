package com.energymonitor.home.application.command;

/**
 * Input of {@code ListMembers}: lists the active members of a home.
 *
 * @param userId the actor requesting the list
 * @param homeId the home whose members are listed
 */
public record ListMembersQuery(String userId, String homeId) {
}
