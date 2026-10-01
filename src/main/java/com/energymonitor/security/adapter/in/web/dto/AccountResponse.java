package com.energymonitor.security.adapter.in.web.dto;

import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.domain.model.UserStatus;

/**
 * Public view of an account.
 *
 * <p>This is the only account shape that leaves the HTTP layer, and it is assembled field by
 * field from the domain object. Because the mapping is explicit and closed, no field can
 * appear in a response by accident: there is no {@code passwordHash}, no session, no reset
 * token and no persistence detail on this record, and reflective serialization of a domain
 * aggregate never happens because a domain object is never handed to the message converter.
 *
 * <p>{@code profileImage} is the one addition since: the avatar is account state rather than a
 * credential, so a client needs it to render the account, and it is published from both
 * mapping paths so login and a profile update report it identically.
 *
 * @param idUser    the account identifier
 * @param idPerson  the owning person identifier
 * @param email     the account address
 * @param status    the account status
 * @param lastLogin    when the account last authenticated, {@code null} when never
 * @param profileImage the avatar shown for the account, {@code null} when none is set
 */
public record AccountResponse(
        String idUser,
        String idPerson,
        String email,
        UserStatus status,
        java.time.Instant lastLogin,
        String profileImage) {

    /**
     * Maps the credential-free identity returned by the authentication flow.
     *
     * @param identity the authenticated identity
     * @return the public view
     */
    public static AccountResponse from(AuthenticatedUser identity) {
        return new AccountResponse(identity.idUser(), identity.idPerson(),
                identity.email().value(), identity.status(), identity.lastLoginAt(),
                identity.profileImage());
    }

    /**
     * Maps the aggregate returned by the registration and lookup input ports.
     *
     * <p>Only the fields listed here are copied; the account's password hash is read from the
     * aggregate only to be left behind.
     *
     * @param user the domain aggregate
     * @return the public view
     */
    public static AccountResponse from(User user) {
        return new AccountResponse(user.idUser(), user.idPerson(), user.email().value(),
                user.status(), user.lastLoginAt().orElse(null),
                user.profileImage().orElse(null));
    }
}
