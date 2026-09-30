package com.energymonitor.security.application.result;

/**
 * What a refresh attempt produced.
 *
 * <p>The new secret is only present on {@link RefreshStatus#ROTATED}. On a rejection it is
 * {@code null}, and on a detected replay it is {@code null} as well, because the whole point of
 * that branch is that nothing new is handed out.
 *
 * @param status        how the attempt ended
 * @param rawRefreshToken the replacement secret, {@code null} unless the attempt rotated
 * @param idUserSession the session the credentials belong to, {@code null} on rejection
 */
public record RefreshSessionResult(RefreshStatus status, String rawRefreshToken,
                                   String idUserSession) {

    /** @return whether a new secret was issued */
    public boolean issued() {
        return status == RefreshStatus.ROTATED;
    }
}
