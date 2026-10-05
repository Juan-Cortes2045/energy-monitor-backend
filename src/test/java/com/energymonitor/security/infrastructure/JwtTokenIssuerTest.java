package com.energymonitor.security.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Behaviour of the JWT issuer and of the encoder/decoder pair it is built on.
 *
 * <p>The token is the credential a caller presents on every protected request, so these
 * tests read the issued token back with the application's own decoder and assert both what it
 * carries and what it must never carry.
 */
class JwtTokenIssuerTest {

    private static final String SECRET = "test-only-signing-secret-0123456789abcdef";
    private static final String ISSUER = "https://energy-monitor-backend";
    private static final Duration TTL = Duration.ofMinutes(15);

    /**
     * Pinned near the current instant rather than to a literal date: the decoder validates
     * expiry against the system clock, so a token issued at a fixed date in the past would be
     * rejected before any claim could be read.
     */
    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(encoderFor(SECRET),
            new SecurityJwtProperties(SECRET, ISSUER, TTL), clock);

    private static AuthenticatedUser identity() {
        return new AuthenticatedUser("USR0000001", "PER0000001",
                Email.of("someone@example.com"), UserStatus.ACTIVE, NOW, null);
    }

    private static JwtEncoder encoderFor(String secret) {
        return NimbusJwtEncoder.withSecretKey(keyFrom(secret))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    private static JwtDecoder decoderFor(String secret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(keyFrom(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    private static SecretKey keyFrom(String secret) {
        return new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256");
    }

    private Jwt decode(String token) {
        return decoderFor(SECRET).decode(token);
    }

    @Test
    void issuesATokenItsOwnDecoderAccepts() {
        String token = issuer.issue(identity());

        Jwt decoded = decode(token);

        assertEquals("USR0000001", decoded.getSubject());
    }

    @Test
    void carriesTheAccountAndPersonIdentifiers() {
        Jwt decoded = decode(issuer.issue(identity()));

        assertEquals("USR0000001", decoded.getSubject());
        assertEquals("PER0000001", decoded.getClaim(JwtTokenIssuer.CLAIM_PERSON_ID));
    }

    @Test
    void carriesTheAccountAddress() {
        Jwt decoded = decode(issuer.issue(identity()));

        assertEquals("someone@example.com", decoded.getClaim(JwtTokenIssuer.CLAIM_EMAIL));
    }

    @Test
    void carriesTheStandardClaims() {
        Jwt decoded = decode(issuer.issue(identity()));

        assertEquals(ISSUER, decoded.getIssuer().toString());
        assertEquals(NOW, decoded.getIssuedAt());
        assertEquals(NOW.plus(TTL), decoded.getExpiresAt());
        assertFalse(decoded.getId().isBlank(), "jti lets an issued token be traced");
    }

    @Test
    void expiresExactlyWhenTheConfiguredLifetimeElapses() {
        Jwt decoded = decode(issuer.issue(identity()));

        assertEquals(TTL.toSeconds(),
                decoded.getExpiresAt().getEpochSecond() - decoded.getIssuedAt().getEpochSecond());
    }

    @Test
    void reportsTheConfiguredLifetimeToTheCaller() {
        assertEquals(TTL, issuer.accessTokenTtl());
    }

    @Test
    void neverCarriesRolesOrPermissions() {
        // Authorization is answered against live state, so a permission baked into a token
        // would keep granting access after the permission was revoked.
        String claims = decode(issuer.issue(identity())).getClaims().toString().toLowerCase();

        assertFalse(claims.contains("password"));
        assertFalse(claims.contains("roles"));
        assertFalse(claims.contains("authorities"));
        assertFalse(claims.contains("permission"));
    }

    @Test
    void neverCarriesThePasswordHash() {
        Jwt decoded = decode(issuer.issue(identity()));

        assertFalse(decoded.getClaims().values().stream()
                        .anyMatch(value -> String.valueOf(value).contains("$2")),
                "A BCrypt hash must never appear in a token");
    }

    @Test
    void givesEachIssuedTokenADistinctIdentifier() {
        String first = issuer.issue(identity());
        String second = issuer.issue(identity());

        assertNotEquals(decode(first).getId(), decode(second).getId());
        assertNotEquals(first, second);
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        String foreignToken = new JwtTokenIssuer(encoderFor("a-completely-different-secret-98765"),
                new SecurityJwtProperties("a-completely-different-secret-98765", ISSUER, TTL),
                clock).issue(identity());

        assertThrows(Exception.class, () -> decode(foreignToken),
                "A token signed with another key must not be accepted");
    }

    @Test
    void rejectsATokenFromAnotherIssuerEvenWithAValidSignature() {
        // The signing key alone must not be enough: a token minted for a different system by
        // the same holder of the key is still not ours.
        String foreignIssuer = new JwtTokenIssuer(encoderFor(SECRET),
                new SecurityJwtProperties(SECRET, "some-other-service", TTL), clock)
                .issue(identity());

        assertThrows(Exception.class, () -> decode(foreignIssuer));
    }

    @Test
    void rejectsATokenThatHasAlreadyExpired() {
        Instant longAgo = NOW.minus(Duration.ofHours(2));
        String expired = new JwtTokenIssuer(encoderFor(SECRET),
                new SecurityJwtProperties(SECRET, ISSUER, TTL),
                Clock.fixed(longAgo, ZoneOffset.UTC)).issue(identity());

        assertThrows(Exception.class, () -> decode(expired));
    }

    @Test
    void refusesToBuildAKeyFromAShortSecret() {
        // The fail-fast behaviour is what stops a deployment from silently signing with a
        // weak or well-known key.
        JwtConfiguration configuration = new JwtConfiguration();
        SecurityJwtProperties tooShort = new SecurityJwtProperties("short", ISSUER, TTL);

        assertThrows(IllegalStateException.class, () -> configuration.jwtEncoder(tooShort));
        assertThrows(IllegalStateException.class, () -> configuration.jwtDecoder(tooShort));
    }

    @Test
    void refusesToBuildAKeyWhenNoSecretIsConfigured() {
        JwtConfiguration configuration = new JwtConfiguration();
        SecurityJwtProperties missing = new SecurityJwtProperties(null, ISSUER, TTL);

        assertThrows(IllegalStateException.class, () -> configuration.jwtEncoder(missing));
    }

    @Test
    void acceptsASecretOfExactlyTheMinimumLength() {
        String minimum = "0".repeat(32);
        JwtConfiguration configuration = new JwtConfiguration();
        SecurityJwtProperties properties = new SecurityJwtProperties(minimum, ISSUER, TTL);

        assertTrue(configuration.jwtEncoder(properties) != null);
        assertTrue(configuration.jwtDecoder(properties) != null);
    }
}
