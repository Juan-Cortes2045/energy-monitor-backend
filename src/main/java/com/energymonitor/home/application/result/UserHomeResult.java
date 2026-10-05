package com.energymonitor.home.application.result;

import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;

/**
 * Result of a membership query.
 *
 * @param userId   identifier of the user
 * @param homeId   identifier of the home
 * @param role     role inside the home
 * @param favorite whether the home is marked as favorite
 */
public record UserHomeResult(String userId, String homeId, Role role, boolean favorite) {

    /**
     * Creates a result from a domain object.
     *
     * @param membership the domain object
     * @return the result
     */
    public static UserHomeResult from(UserHome membership) {
        return new UserHomeResult(membership.userId(), membership.homeId(),
                membership.role(), membership.isFavorite());
    }
}
