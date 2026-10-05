package com.energymonitor.home.adapter.in.web.dto;

import com.energymonitor.home.domain.model.Role;

/**
 * Response DTO for a user-home membership.
 *
 * @param userId   identifier of the user
 * @param homeId   identifier of the home
 * @param role     role inside the home
 * @param favorite whether the home is marked as favorite
 */
public record UserHomeResponse(
        String userId,
        String homeId,
        Role role,
        boolean favorite
) {
}
