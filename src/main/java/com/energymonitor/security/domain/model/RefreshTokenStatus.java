package com.energymonitor.security.domain.model;

/**
 * Where a refresh token generation stands in its lifecycle.
 *
 * <p>A generation is created {@link #ACTIVE} and never returns to it. Every other value is
 * terminal, and the distinction between them is what turns a replay into a detectable event
 * rather than a failed lookup.
 */
public enum RefreshTokenStatus {

    /**
     * The only status that authorises anything. Presenting an active token rotates it.
     */
    ACTIVE,

    /**
     * Already presented once and replaced by a later generation of the same family.
     *
     * <p>This is the status that makes theft detectable: the token is genuine, it is simply no
     * longer the current one. Presenting it again is replay, not a typo, and the family is
     * treated as compromised.
     */
    ROTATED,

    /**
     * Invalidated by a security action, such as a password change or the family revocation
     * that follows a replay.
     */
    REVOKED,

    /**
     * Past {@code expiresAt}. Derived from the clock rather than stored, because a token
     * ageing out is not a decision anybody took.
     */
    EXPIRED
}
