package com.energymonitor.home.adapter.in.web;

import com.energymonitor.home.application.exception.MissingUserIdentityException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the current user's identity from the HTTP request.
 *
 * <p><strong>TEMPORARY AND INSECURE:</strong> This resolver reads the user ID from the
 * {@code X-User-Id} header, which is trivially spoofable. It exists only to unblock
 * development of the home module before the {@code security} module exposes authentication.
 *
 * <p>When {@code security} is ready, this class must be replaced by a proper
 * {@code CurrentUserPrincipal} or similar that reads the authenticated principal from
 * the Spring Security context or from validated JWT claims.
 */
@Component
public class CurrentUserResolver {

    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Resolves the current user's ID from the request header.
     *
     * @return the user ID
     * @throws MissingUserIdentityException when the header is missing (401)
     */
    public String resolveCurrentUserId() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new MissingUserIdentityException("no request context available");
        }
        var request = attributes.getRequest();
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new MissingUserIdentityException("missing " + USER_ID_HEADER + " header");
        }
        return userId;
    }
}
