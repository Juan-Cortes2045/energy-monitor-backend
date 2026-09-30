package com.energymonitor.security.application.result;

import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.UserStatus;
import java.time.Instant;

/**
 * Identity of the account that has just been authenticated.
 *
 * <p>A minimal, immutable snapshot of what a successful attempt establishes: who the caller
 * is and what state the account was in. It deliberately carries no {@code PasswordHash}, no
 * session or reset token, no roles and no mutable domain behaviour, so nothing credential
 * bearing can reach a caller through the authentication result.
 *
 * @param idUser      the account identifier
 * @param idPerson    the identifier of the owning person
 * @param email       the account address
 * @param status      the account status at the moment of authentication
 * @param lastLoginAt when this successful login was recorded
 */
public record AuthenticatedUser(
        String idUser,
        String idPerson,
        Email email,
        UserStatus status,
        Instant lastLoginAt
) {
}
