package com.energymonitor.home.application.result;

import com.energymonitor.home.application.port.out.UserDirectoryPort.UserSummary;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;

/**
 * Result of a membership query.
 *
 * @param userId   identifier of the user
 * @param homeId   identifier of the home
 * @param role     role inside the home
 * @param favorite whether the home is marked as favorite
 * @param name     first name of the user, {@code null} when not resolved
 * @param lastName last name of the user, {@code null} when not resolved
 * @param email    address of the user, {@code null} when not resolved
 */
public record UserHomeResult(String userId, String homeId, Role role, boolean favorite,
                             String name, String lastName, String email) {

    /**
     * Creates a result from a domain object.
     *
     * @param membership the domain object
     * @return the result
     */
    public static UserHomeResult from(UserHome membership) {
        return from(membership, null);
    }

    /**
     * Creates a result from a membership and the person behind it.
     *
     * @param membership the domain object
     * @param user       the member's name and address, or {@code null} when unknown
     * @return the result
     */
    public static UserHomeResult from(UserHome membership, UserSummary user) {
        return new UserHomeResult(membership.userId(), membership.homeId(),
                membership.role(), membership.isFavorite(),
                user == null ? null : user.name(),
                user == null ? null : user.lastName(),
                user == null ? null : user.email());
    }
}
