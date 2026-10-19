package com.energymonitor.home.adapter.in.web;

import com.energymonitor.home.application.exception.MissingUserIdentityException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller of a home endpoint from the validated access token.
 *
 * <p><strong>Why the token's subject and nothing else.</strong> The resource server has already
 * checked the token's signature, issuer and expiry before a controller runs, and the security
 * module issues it with the account identifier ({@code user.id_user}) as {@code sub}. That is the
 * only statement about who the caller is that the caller cannot forge. A request header or body
 * field naming a user is just a value the client chose, so reading one would let any holder of a
 * valid token act on someone else's homes. No header is consulted here, deliberately: a client
 * that still sends {@code X-User-Id} has it ignored rather than trusted.
 *
 * <p>This is the same rule {@code security} follows in its own controllers, which take the
 * account from {@code Authentication#getName()}. It is applied by reading Spring Security's
 * context rather than by calling the security module, so {@code home} keeps depending on
 * {@code security::api} only.
 *
 * <p>An unauthenticated request never reaches a home controller, because the filter chain answers
 * {@code 401} first. The check below is the backstop for a context without a JWT (a test slice,
 * or a route accidentally opened later), and it fails as {@code 401} rather than as a
 * {@code NullPointerException} turned into {@code 500}.
 */
@Component
public class CurrentUserResolver {

    /**
     * Resolves the identifier of the authenticated caller.
     *
     * @return the {@code sub} claim of the access token, which is the caller's {@code id_user}
     * @throws MissingUserIdentityException when the request carries no validated JWT (401)
     */
    public String resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new MissingUserIdentityException("no authenticated user in the request");
        }
        String subject = token.getToken().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MissingUserIdentityException("the access token carries no subject");
        }
        return subject;
    }
}
