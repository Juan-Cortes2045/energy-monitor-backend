package com.energymonitor.home.adapter.in.web.dto;

import com.energymonitor.home.application.result.UserHomeResult;
import com.energymonitor.home.domain.model.Role;

/**
 * Response DTO for a user-home membership.
 *
 * @param userId   identifier of the user
 * @param homeId   identifier of the home
 * @param role     role inside the home
 * @param favorite whether the home is marked as favorite
 * @param name     first name of the user, {@code null} when not resolved
 * @param lastName last name of the user, {@code null} when not resolved
 * @param email    address of the user, {@code null} when not resolved
 */
public record UserHomeResponse(
        String userId,
        String homeId,
        Role role,
        boolean favorite,
        String name,
        String lastName,
        String email
) {

    public static UserHomeResponse from(UserHomeResult result) {
        return new UserHomeResponse(result.userId(), result.homeId(), result.role(),
                result.favorite(), result.name(), result.lastName(), result.email());
    }
}
