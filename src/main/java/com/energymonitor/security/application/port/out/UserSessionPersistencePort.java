package com.energymonitor.security.application.port.out;

import com.energymonitor.security.domain.model.UserSession;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting {@link UserSession}.
 */
public interface UserSessionPersistencePort {

    /**
     * Inserts a session, or updates the stored row when the same identifier already exists.
     *
     * @param session the domain object
     * @return the same domain object
     */
    UserSession save(UserSession session);

    /**
     * Finds the active session by identifier.
     *
     * @param idUserSession the identifier
     * @return the domain object, empty when soft-deleted or missing
     */
    Optional<UserSession> findActive(String idUserSession);

    /**
     * Finds the active session by refresh token value.
     *
     * @param refreshToken the token value
     * @return the domain object, empty when soft-deleted, revoked or unknown
     */
    Optional<UserSession> findActiveByRefreshToken(String refreshToken);

    /**
     * Lists the active sessions of a user, most recent first.
     *
     * @param idUser the owner
     * @return the sessions
     */
    List<UserSession> listActiveByUser(String idUser);
}