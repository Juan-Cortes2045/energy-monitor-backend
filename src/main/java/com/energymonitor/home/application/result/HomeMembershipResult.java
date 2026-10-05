package com.energymonitor.home.application.result;

import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Instant;

/**
 * Result of a home query with membership information (role and favorite flag).
 *
 * @param idHome       identifier
 * @param name         display name
 * @param homeTypeId   identifier of the home type
 * @param address      physical address
 * @param accessCode   unique join code
 * @param description  optional description
 * @param creationDate when the home was created
 * @param role         the user's role in the home
 * @param favorite     whether the home is marked as favorite
 */
public record HomeMembershipResult(String idHome, String name, String homeTypeId, String address,
                                   String accessCode, String description, Instant creationDate,
                                   Role role, boolean favorite) {

    /**
     * Creates a result from domain objects.
     *
     * @param home       the home
     * @param membership the user's membership in the home
     * @return the result
     */
    public static HomeMembershipResult from(Home home, UserHome membership) {
        return new HomeMembershipResult(home.idHome(), home.name(), home.homeTypeId(), home.address(),
                home.accessCode(), home.description(), home.creationDate(),
                membership.role(), membership.isFavorite());
    }
}
