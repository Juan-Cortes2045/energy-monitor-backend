package com.energymonitor.security.application.port.out;

import com.energymonitor.security.domain.model.PasswordResetToken;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting {@link PasswordResetToken}.
 */
public interface PasswordResetTokenPersistencePort {

    /**
     * Inserts a token.
     *
     * @param token the domain object
     * @return the same domain object
     */
    PasswordResetToken save(PasswordResetToken token);

    /**
     * Updates the state (used flag) of a stored token.
     *
     * @param token the domain object holding the new state
     * @return the same domain object
     */
    PasswordResetToken update(PasswordResetToken token);

    /**
     * Finds the active token by identifier.
     *
     * @param idResetToken the identifier
     * @return the domain object, empty when soft-deleted or missing
     */
    Optional<PasswordResetToken> findActive(String idResetToken);

    /**
     * Finds the active token by its opaque value.
     *
     * @param resetToken the value
     * @return the domain object, empty when soft-deleted or unknown
     */
    Optional<PasswordResetToken> findActiveByValue(String resetToken);

    /**
     * Lists the active tokens of a user, newest first.
     *
     * @param idUser the recipient
     * @return the pending tokens
     */
    List<PasswordResetToken> listActiveByUser(String idUser);
}