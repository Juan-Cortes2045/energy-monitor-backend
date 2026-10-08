package com.energymonitor.home.application.result;

import com.energymonitor.home.application.port.out.UserDirectoryPort.UserSummary;
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
 * @param userResponsible         first name of the home's owner, {@code null} when unknown
 * @param userResponsibleLastName last name of the home's owner, {@code null} when unknown
 * @param userResponsibleEmail    address of the home's owner, {@code null} when unknown
 */
public record HomeMembershipResult(String idHome, String name, String homeTypeId, String address,
                                   String accessCode, String description, Instant creationDate,
                                   Role role, boolean favorite, String userResponsible,
                                   String userResponsibleLastName, String userResponsibleEmail) {

    /**
     * Creates a result from domain objects.
     *
     * @param home       the home
     * @param membership the user's membership in the home
     * @param owner      who owns the home, or {@code null} when unknown
     * @return the result
     */
    public static HomeMembershipResult from(Home home, UserHome membership, UserSummary owner) {
        return new HomeMembershipResult(home.idHome(), home.name(), home.homeTypeId(), home.address(),
                home.accessCode(), home.description(), home.creationDate(),
                membership.role(), membership.isFavorite(),
                owner == null ? null : owner.name(),
                owner == null ? null : owner.lastName(),
                owner == null ? null : owner.email());
    }
}
