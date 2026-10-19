package com.energymonitor.home.application.command;

/**
 * Input of {@code ListHomes}: lists all homes where the user is a member.
 *
 * @param userId the user whose homes are listed
 */
public record ListHomesQuery(String userId) {
}
