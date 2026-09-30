package com.energymonitor.security.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Persisted authenticated session of a {@link User}. The revocable unit backing refresh tokens.
 *
 * <p>A session is valid only while it has not been revoked, not been closed and not expired
 * (INV-011). The refresh token value is stored opaquely: nothing about signing it belongs to
 * the domain.
 *
 * <p>Maps to the {@code user_session} table.
 */
public class UserSession {

    private static final int REFRESH_TOKEN_MAX = 255;
    private static final int IP_ADDRESS_MAX = 45;
    private static final int USER_AGENT_MAX = 150;

    private final String idUserSession;
    private final String idUser;
    private final String refreshToken;
    private final Instant createdAt;
    private final Instant expirationAt;
    private final String ipAddress;
    private final String userAgent;
    private boolean revoked;
    private Instant closedAt;

    /**
     * @param idUserSession identifier, {@code VARCHAR(10)}
     * @param idUser        owning user
     * @param refreshToken  opaque refresh token value
     * @param createdAt     session start
     * @param expirationAt  session expiry
     * @param ipAddress     optional client IP
     * @param userAgent     optional client agent
     * @param revoked       whether it was already revoked
     * @param closedAt      closure instant, {@code null} while open
     * @throws IllegalStateException if the expiration is not after the creation
     */
    public UserSession(String idUserSession, String idUser, String refreshToken,
                       Instant createdAt, Instant expirationAt, String ipAddress,
                       String userAgent, boolean revoked, Instant closedAt) {
        this.idUserSession = Preconditions.text(idUserSession, "idUserSession");
        this.idUser = Preconditions.text(idUser, "idUser");
        this.refreshToken = Preconditions.text(refreshToken, REFRESH_TOKEN_MAX, "refreshToken");
        this.createdAt = Preconditions.notNull(createdAt, "createdAt");
        this.expirationAt = Preconditions.notNull(expirationAt, "expirationAt");
        Preconditions.expirationAfter(createdAt, expirationAt);
        this.ipAddress = Preconditions.optionalText(ipAddress, IP_ADDRESS_MAX, "ipAddress");
        this.userAgent = Preconditions.optionalText(userAgent, USER_AGENT_MAX, "userAgent");
        this.revoked = revoked;
        this.closedAt = closedAt;
    }

    /**
     * Opens a new session.
     *
     * @param idUserSession identifier
     * @param idUser        owning user
     * @param refreshToken  opaque refresh token
     * @param createdAt     session start
     * @param expirationAt  session expiry
     * @param ipAddress     optional client IP
     * @param userAgent     optional client agent
     * @return a fresh, open session
     * @throws IllegalStateException if the expiration is not after the creation
     */
    public static UserSession open(String idUserSession, String idUser, String refreshToken,
                                   Instant createdAt, Instant expirationAt, String ipAddress,
                                   String userAgent) {
        return new UserSession(idUserSession, idUser, refreshToken, createdAt, expirationAt,
                ipAddress, userAgent, false, null);
    }

    /** @return the identifier */
    public String idUserSession() {
        return idUserSession;
    }

    /** @return owning user identifier */
    public String idUser() {
        return idUser;
    }

    /** @return the opaque refresh token value */
    public String refreshToken() {
        return refreshToken;
    }

    /** @return session start instant */
    public Instant createdAt() {
        return createdAt;
    }

    /** @return session expiry instant */
    public Instant expirationAt() {
        return expirationAt;
    }

    /** @return client IP, empty when not captured */
    public Optional<String> ipAddress() {
        return Optional.ofNullable(ipAddress);
    }

    /** @return client user agent, empty when not captured */
    public Optional<String> userAgent() {
        return Optional.ofNullable(userAgent);
    }

    /** @return whether the session was revoked */
    public boolean isRevoked() {
        return revoked;
    }

    /** @return closure instant, empty while the session is open */
    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }

    /**
     * Whether the session has reached its expiry.
     *
     * @param now reference instant
     * @return {@code true} when expired
     */
    public boolean isExpired(Instant now) {
        Preconditions.notNull(now, "now");
        return !now.isBefore(expirationAt);
    }

    /**
     * Whether the session may still be used: not revoked, not closed, not expired (INV-011).
     *
     * @param now reference instant
     * @return {@code true} when the session is active
     */
    public boolean isActive(Instant now) {
        Preconditions.notNull(now, "now");
        return !revoked && closedAt == null && !isExpired(now);
    }

    /** Revokes the session. Revocation is terminal. */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * Closes the session at the given instant, e.g. on logout.
     *
     * @param instant closure time
     */
    public void close(Instant instant) {
        Preconditions.notNull(instant, "instant");
        this.closedAt = instant;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserSession that)) {
            return false;
        }
        return idUserSession.equals(that.idUserSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUserSession);
    }

    @Override
    public String toString() {
        return "UserSession{idUserSession='" + idUserSession + "', idUser='" + idUser
                + "', revoked=" + revoked + "}";
    }
}
