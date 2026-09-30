package com.energymonitor.security.application.port.out;

import com.energymonitor.security.domain.model.UserSession;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting {@link UserSession}.
 *
 * <h2>Temporary coupling to the refresh token</h2>
 *
 * <p>A session owns a refresh token for now, so this port still exposes it. That coupling is
 * scheduled to end: refresh tokens move to their own table with their own lineage, and once
 * that happens the token leaves this aggregate and two things change here.
 *
 * <ul>
 *   <li>{@link #findActiveByRefreshToken(String)} stops making sense, because a token will no
 *       longer identify a session row. Resolving a token becomes a lookup on the token itself,
 *       which then points at the session it belongs to. The method is kept for now because
 *       removing it would break the adapter for no benefit, and it has no production caller.</li>
 *   <li>Session persistence stops being responsible for the credential value at all, so this
 *       port becomes purely about the login: open it, find it, list a user's, close it.</li>
 * </ul>
 *
 * <p>No partial model is introduced to smooth the transition. A half-built token concept now
 * would have to be thrown away by the work that actually needs it.
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