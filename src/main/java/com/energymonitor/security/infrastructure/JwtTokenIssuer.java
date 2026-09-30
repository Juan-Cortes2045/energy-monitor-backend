package com.energymonitor.security.infrastructure;

import com.energymonitor.security.application.result.AuthenticatedUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues the JWT access token for an authenticated caller.
 *
 * <p>Everything JWT-specific lives here: the application layer neither builds nor reads a
 * token, and controllers only ever pass the resulting opaque string around.
 *
 * <p>The claim set is deliberately minimal and carries no credential material:
 *
 * <ul>
 *   <li>{@code sub} - the account identifier, the stable handle every other module uses.</li>
 *   <li>{@code pid} - the owning person identifier.</li>
 *   <li>{@code email} - the account address the caller needs to render itself.</li>
 *   <li>{@code iss}, {@code iat}, {@code exp}, {@code jti} - registered standard claims.</li>
 * </ul>
 *
 * <p>Roles and permission codes are intentionally absent. Authorization is answered by the
 * {@code CheckPermission} input port against live state, so baking a permission list into a
 * token would let a stale claim keep granting access after a revocation.
 */
@Component
public class JwtTokenIssuer {

    /** Claim holding the owning person identifier, alongside the standard {@code sub}. */
    public static final String CLAIM_PERSON_ID = "pid";

    /** Claim holding the account address. */
    public static final String CLAIM_EMAIL = "email";

    private final JwtEncoder encoder;
    private final SecurityJwtProperties properties;
    private final Clock clock;

    public JwtTokenIssuer(JwtEncoder encoder, SecurityJwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Issues a signed access token for a successfully authenticated account.
     *
     * @param identity the authenticated identity, never the {@code User} aggregate
     * @return the encoded, signed token
     */
    public String issue(AuthenticatedUser identity) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(identity.idUser())
                .claim(CLAIM_PERSON_ID, identity.idPerson())
                .claim(CLAIM_EMAIL, identity.email().value())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * How long an issued access token stays valid.
     *
     * @return the configured lifetime, so the client knows when to refresh
     */
    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }
}
