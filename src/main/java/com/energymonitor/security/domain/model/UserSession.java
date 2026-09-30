package com.energymonitor.security.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Persisted authenticated session of a {@link User}.
 *
 * <p>A session is valid only while it has not been revoked, not been closed and not expired
 * (INV-011). The refresh token value is stored opaquely: nothing about signing it belongs to
 * the domain.
 *
 * <h2>Revocation and closure are different events</h2>
 *
 * <p>Both end the session, and the difference is why, which decides what a later operator can
 * conclude from the row:
 *
 * <ul>
 *   <li>{@link #revoke()} records a <em>security invalidation</em>: the credential is or may be
 *       in someone else's hands. Reuse of a superseded refresh token, a password reset, an
 *       administrative decision or a suspected theft all belong here.</li>
 *   <li>{@link #close(Instant)} records <em>ordinary termination</em>: the holder walked away.
 *       Logout is the ordinary case.</li>
 * </ul>
 *
 * <p>They are kept distinct because collapsing them loses the ability to tell, months later,
 * whether an account was attacked or merely used. A revoked session is terminal and is never
 * expected to come back; a closed one has simply ended.
 *
 * <p>Known limitation, carried into the next step: neither operation records <em>when</em> the
 * session was revoked or <em>why</em>. {@code revoke()} takes no instant and leaves
 * {@code closedAt} unset, so today a revocation is distinguishable from a closure only by the
 * {@code revoked} flag, and an audit reader cannot see the revocation time. Logout currently
 * calls {@code revoke()} and should call {@code close(Instant)} instead. Both gaps are
 * deliberately left untouched here and are resolved with the rest of the session capability.
 *
 * <p>Maps to the {@code user_session} table.
 *
 * <h2>Why refresh tokens will move to their own table</h2>
 *
 * <p>{@code refreshToken} is a field of this aggregate today, and that is the design decision
 * this class records for the next step. At present one row is <em>both</em> the session and
 * the refresh token, which is why rotation cannot be modelled: there is no way to replace the
 * token while keeping the session, because the session <em>is</em> the token, and a rotated
 * token would have to become a second session for the same login.
 *
 * <p>Rotation also needs to answer a question this structure cannot answer. Given a lineage
 *
 * <pre>
 *   R1 -&gt; R2 -&gt; R3      (one family)
 * </pre>
 *
 * a presentation of {@code R1} after {@code R3} is a theft signal, and answering it requires
 * knowing that {@code R1} belongs to the same family as the current token. A single row can
 * only remember its immediate predecessor, so after two rotations the oldest token is
 * indistinguishable from an expired or a forged one.
 *
 * <p>The decision is therefore to keep the session here and move the token lineage into a
 * separate {@code refresh_token} table that references this one. Each row there carries the
 * session it belongs to, the family it descends from, and the token that replaced it, which
 * makes both the current and the superseded token addressable, lets a replay of any ancestor
 * be recognised, and lets the whole family be revoked in one operation.
 *
 * <p>Separating them also keeps the extractability this modular monolith is being shaped for:
 * session lifetime and token rotation are different concerns with different rates of change,
 * and a table that owns only the token lineage can move to an authentication service without
 * dragging session metadata along.
 *
 * <p>Nothing of that exists yet. This class still holds a single {@code refreshToken} and the
 * schema still has one row per session, because the field and the column have to be replaced
 * together and that replacement belongs with the refresh flow rather than ahead of it.
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

    /**
     * Revokes the session as a security measure.
     *
     * <p>Use this when the credential is or may be compromised: reuse of a superseded refresh
     * token, a password change or reset, an administrative revocation. Revocation is terminal
     * and idempotent, and it is deliberately <em>not</em> the operation for an ordinary logout,
     * which is a {@link #close(Instant)} instead.
     *
     * <p>Not recording the instant or the reason is a known gap, deferred to the session
     * capability work: this method takes no time argument, so an operator reading the row later
     * cannot tell when the session was revoked or why.
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * Closes the session at the given instant, ending it normally.
     *
     * <p>Use this for logout and any other voluntary end of use. Closing is not a security
     * judgement about the credential, which is what separates it from {@link #revoke()}.
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
