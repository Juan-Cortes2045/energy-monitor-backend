package com.energymonitor.security.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Single-use token allowing a {@link User} to recover their password.
 *
 * <p>The domain tracks validity only. Generating the random token and mailing it belong to
 * the security adapter, which will arrive at this object already populated.
 *
 * <p>Maps to the {@code password_reset_token} table.
 */
public class PasswordResetToken {

    private static final int TOKEN_MAX = 100;

    private final String idResetToken;
    private final String idUser;
    private final String resetToken;
    private final Instant createdAt;
    private final Instant expirationAt;
    private boolean used;

    /**
     * Issues a token. It starts unused and, provided the expiration is in the future, valid.
     *
     * @param idResetToken identifier, {@code VARCHAR(10)}
     * @param idUser       user the token was issued to
     * @param resetToken   the random token value
     * @param createdAt    issue instant
     * @param expirationAt expiry instant, must be after {@code createdAt}
     * @throws IllegalStateException if the expiration is not after the creation
     */
    public PasswordResetToken(String idResetToken, String idUser, String resetToken,
                              Instant createdAt, Instant expirationAt) {
        this.idResetToken = Preconditions.text(idResetToken, "idResetToken");
        this.idUser = Preconditions.text(idUser, "idUser");
        this.resetToken = Preconditions.text(resetToken, TOKEN_MAX, "resetToken");
        this.createdAt = Preconditions.notNull(createdAt, "createdAt");
        this.expirationAt = Preconditions.notNull(expirationAt, "expirationAt");
        Preconditions.expirationAfter(createdAt, expirationAt);
        this.used = false;
    }

    /**
     * Rehydrates a stored token, used state included.
     *
     * <p>Enforces the same validity window as {@link #PasswordResetToken(String, String, String,
     * Instant, Instant)}. Coming from the database does not excuse an impossible state: a row
     * whose expiry precedes its creation is corrupt, and the domain refuses to represent it
     * rather than letting it travel through the application.
     *
     * @param idResetToken identifier
     * @param idUser       user the token belongs to
     * @param resetToken   the token value
     * @param createdAt    issue instant
     * @param expirationAt expiry instant
     * @param used         whether it has already been used
     * @throws IllegalStateException if the expiration is not after the creation
     */
    public PasswordResetToken(String idResetToken, String idUser, String resetToken,
                              Instant createdAt, Instant expirationAt, boolean used) {
        this.idResetToken = Preconditions.text(idResetToken, "idResetToken");
        this.idUser = Preconditions.text(idUser, "idUser");
        this.resetToken = Preconditions.text(resetToken, TOKEN_MAX, "resetToken");
        this.createdAt = Preconditions.notNull(createdAt, "createdAt");
        this.expirationAt = Preconditions.notNull(expirationAt, "expirationAt");
        Preconditions.expirationAfter(createdAt, expirationAt);
        this.used = used;
    }

    /** @return the identifier */
    public String idResetToken() {
        return idResetToken;
    }

    /** @return the user the token was issued to */
    public String idUser() {
        return idUser;
    }

    /** @return the token value */
    public String resetToken() {
        return resetToken;
    }

    /** @return issue instant */
    public Instant createdAt() {
        return createdAt;
    }

    /** @return expiry instant */
    public Instant expirationAt() {
        return expirationAt;
    }

    /** @return whether the token has already been consumed */
    public boolean isUsed() {
        return used;
    }

    /**
     * Time remaining before expiry.
     *
     * @param now reference instant
     * @return the remaining duration, zero when already expired
     */
    public Duration remainingValidity(Instant now) {
        Preconditions.notNull(now, "now");
        Duration remaining = Duration.between(now, expirationAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Whether the token is past its expiry at the given instant (INV-009).
     *
     * @param now reference instant
     * @return {@code true} when expired
     */
    public boolean isExpired(Instant now) {
        Preconditions.notNull(now, "now");
        return !now.isBefore(expirationAt);
    }

    /**
     * Whether the token may still be redeemed: neither used nor expired.
     *
     * @param now reference instant
     * @return {@code true} when the token is usable
     */
    public boolean isValid(Instant now) {
        return !used && !isExpired(now);
    }

    /**
     * Consumes the token. A used token can never be reused (INV-010).
     *
     * @throws IllegalStateException if the token was already used
     */
    public void markUsed() {
        if (used) {
            throw new IllegalStateException("password reset token has already been used");
        }
        this.used = true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PasswordResetToken that)) {
            return false;
        }
        return idResetToken.equals(that.idResetToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idResetToken);
    }

    @Override
    public String toString() {
        return "PasswordResetToken{idResetToken='" + idResetToken + "', idUser='" + idUser
                + "', used=" + used + "}";
    }
}
