package com.energymonitor.security.infrastructure;

import com.energymonitor.security.application.command.AuthenticateUserCommand;
import com.energymonitor.security.application.command.CreateUserSessionCommand;
import com.energymonitor.security.application.port.in.AuthenticateUser;
import com.energymonitor.security.application.port.in.CreateUserSession;
import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.domain.model.UserSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the login exchange: authenticate, open a session, issue an access token.
 *
 * <p>The three steps are ordered by meaning rather than by convenience, and none of them
 * contains business logic of its own, so none of it belongs in a controller:
 *
 * <ol>
 *   <li>{@link AuthenticateUser} classifies the attempt and, on success, returns a
 *       credential-free {@link AuthenticatedUser}.</li>
 *   <li>{@link CreateUserSession} opens the refresh-token session for that account.</li>
 *   <li>{@link JwtTokenIssuer} signs the short-lived access token.</li>
 * </ol>
 *
 * <p>A rejected attempt stops at the first step, so it never creates a session row and never
 * mints a token. The rejection comes back as an empty {@link Optional} and the delivery layer
 * decides how to present it, which keeps the account-enumeration policy out of this class.
 */
@Component
public class SecurityLoginFlow {

    private final AuthenticateUser authenticateUser;
    private final CreateUserSession createUserSession;
    private final JwtTokenIssuer tokenIssuer;

    public SecurityLoginFlow(AuthenticateUser authenticateUser, CreateUserSession createUserSession,
                             JwtTokenIssuer tokenIssuer) {
        this.authenticateUser = authenticateUser;
        this.createUserSession = createUserSession;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * What a successful login yields.
     *
     * @param accessToken      the signed JWT the client sends as a bearer token
     * @param refreshToken     the opaque session token, currently returned in the login
     *                         response body rather than in an HttpOnly cookie. There is no
     *                         {@code /auth/refresh} endpoint yet, so nothing currently renews
     *                         an access token with it; see {@code LoginResponse} for the
     *                         transport decision left open for the refresh flow.
     * @param expiresInSeconds access token lifetime, so the client knows when to refresh
     * @param account          the authenticated identity
     */
    public record LoginOutcome(String accessToken, String refreshToken, long expiresInSeconds,
                               AuthenticatedUser account) {
    }

    /**
     * Authenticates and, when the attempt succeeds, opens a session and issues a token.
     *
     * @param email       the account address
     * @param rawPassword the plain-text candidate, never stored or logged
     * @param ipAddress   the caller's address, recorded in the audit or error trail
     * @param userAgent   the caller's user agent, stored with the session
     * @return the issued credentials, or empty when the attempt was rejected
     */
    public Optional<LoginOutcome> login(String email, String rawPassword, String ipAddress,
                                        String userAgent) {
        var result = authenticateUser.authenticate(
                new AuthenticateUserCommand(email, rawPassword, ipAddress));
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        AuthenticatedUser identity = result.user().orElseThrow();
        UserSession session = createUserSession.create(
                new CreateUserSessionCommand(identity.idUser(), ipAddress, userAgent));
        return Optional.of(new LoginOutcome(tokenIssuer.issue(identity), session.refreshToken(),
                tokenIssuer.accessTokenTtl().toSeconds(), identity));
    }
}
