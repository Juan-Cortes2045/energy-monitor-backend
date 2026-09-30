package com.energymonitor.security.adapter.in.web.dto;

/**
 * Body of a successful login.
 *
 * <p>Returning tokens is the purpose of this endpoint, so the access and refresh tokens are
 * here by contract. Nothing else is: the account is described by {@link AccountResponse},
 * which has no credential field, and no password hash or persistence detail is present.
 *
 * <h2>Transport of the refresh token</h2>
 *
 * <p>The refresh token is currently returned <strong>in this response body</strong>, not in
 * an HttpOnly cookie. The earlier Javadoc of this record claimed a cookie, which never
 * matched the implementation.
 *
 * <p>Consequences of the current state, recorded so nobody has to infer them:
 *
 * <ul>
 *   <li>The refresh token is a seven-day credential, so a client that stores this body in
 *       script-accessible storage (localStorage, sessionStorage, plain JS state) exposes it
 *       to any XSS on the page. A client that keeps it in memory only narrows the window but
 *       does not remove it.</li>
 *   <li>There is no {@code POST /api/v1/auth/refresh} endpoint yet, so the refresh token
 *       cannot be exchanged for a new access token. It is issued, returned and currently
 *       unusable by the client; only {@code POST /api/v1/auth/logout} acts on a session.</li>
 *   <li>Because the token travels in the body and the client sends it back in a header rather
 *       than relying on an ambient cookie, CSRF does not apply to it today. Disabling CSRF in
 *       {@code SecurityFilterChainConfiguration} is correct for that reason.</li>
 * </ul>
 *
 * <p>The final transport, and with it the CSRF posture, must be settled when the refresh flow
 * is implemented. Moving the token to an HttpOnly cookie is a candidate, not a decision taken
 * here: it would make the token immune to XSS but would make it ambient and therefore require
 * CSRF protection on the refresh endpoint. Both properties are security-relevant, so the
 * choice belongs with the refresh flow, not with this record.
 *
 * @param accessToken  the signed JWT to send as {@code Authorization: Bearer <token>}
 * @param refreshToken the opaque session token, currently returned in this body; see the
 *                     class documentation for its transport and its lack of a renewal flow
 * @param tokenType    always {@code Bearer}
 * @param expiresIn    access token lifetime in seconds
 * @param account      the authenticated account
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AccountResponse account) {
}
