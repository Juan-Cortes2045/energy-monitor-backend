package com.energymonitor.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.energymonitor.security.application.command.AuthenticateUserCommand;
import com.energymonitor.security.application.command.CreateUserSessionCommand;
import com.energymonitor.security.application.port.in.AuthenticateUser;
import com.energymonitor.security.application.port.in.CreateUserSession;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.RefreshTokenHasherPort;
import com.energymonitor.security.application.port.out.RefreshTokenPersistencePort;
import com.energymonitor.security.application.port.out.TokenGeneratorPort;
import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.application.result.AuthenticationResult;
import com.energymonitor.security.application.result.AuthenticationStatus;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.RefreshToken;
import com.energymonitor.security.domain.model.RefreshTokenStatus;
import com.energymonitor.security.domain.model.UserSession;
import com.energymonitor.security.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * The order of the login exchange and what it refuses to do.
 *
 * <p>The two properties that matter are that a rejected attempt creates nothing and mints
 * nothing, and that the account is resolved from the authentication result rather than looked
 * up again, which would let a race between the two reads hand out a session for someone else.
 */
@ExtendWith(MockitoExtension.class)
class SecurityLoginFlowTest {

    private static final String SECRET = "test-only-signing-secret-0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Mock
    private AuthenticateUser authenticateUser;

    @Mock
    private CreateUserSession createUserSession;

    @Mock
    private JwtTokenIssuer tokenIssuer;

    @Mock
    private RefreshTokenPersistencePort refreshTokens;

    @Mock
    private RefreshTokenHasherPort hasher;

    @Mock
    private TokenGeneratorPort secrets;

    @Mock
    private IdentifierGeneratorPort identifiers;

    private SecurityLoginFlow flow;

    /** Builds the flow explicitly so the clock is pinned instead of mocked. */
    private SecurityLoginFlow flowWith(JwtTokenIssuer issuer) {
        return new SecurityLoginFlow(authenticateUser, createUserSession, refreshTokens, hasher,
                secrets, identifiers, issuer, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @BeforeEach
    void setUp() {
        flow = flowWith(tokenIssuer);
    }

    private static AuthenticatedUser identity() {
        return new AuthenticatedUser("USR0000001", "PER0000001",
                Email.of("someone@example.com"), UserStatus.ACTIVE, NOW, null);
    }

    private static UserSession session() {
        return UserSession.open("SES0000001", "USR0000001", NOW,
                NOW.plus(Duration.ofDays(7)), "10.0.0.1", "JUnit");
    }

    private static JwtTokenIssuer realIssuer(Duration ttl) {
        return new JwtTokenIssuer(
                NimbusJwtEncoder.withSecretKey(new javax.crypto.spec.SecretKeySpec(
                        SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
                        .algorithm(MacAlgorithm.HS256)
                        .build(),
                new SecurityJwtProperties(SECRET, "https://energy-monitor-backend", ttl),
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
    }

    private void givenSuccessfulAuthentication() {
        given(authenticateUser.authenticate(any()))
                .willReturn(new AuthenticationResult(AuthenticationStatus.SUCCESS,
                        Optional.of(identity())));
        given(createUserSession.create(any())).willReturn(session());
        given(secrets.generateToken()).willReturn("the-refresh-token");
        given(identifiers.generate()).willReturn("rft0000001");
        given(hasher.hash("the-refresh-token")).willReturn("hash-of-the-refresh-token");
    }

    @Test
    void issuesBothTokensAndTheAccountOnSuccess() {
        givenSuccessfulAuthentication();
        SecurityLoginFlow realFlow = flowWith(realIssuer(Duration.ofMinutes(15)));

        Optional<SecurityLoginFlow.LoginOutcome> outcome = realFlow.login("someone@example.com",
                "StrongPass1!", "10.0.0.1", "JUnit");

        assertTrue(outcome.isPresent());
        assertEquals("the-refresh-token", outcome.orElseThrow().refreshToken());
        assertEquals("USR0000001", outcome.orElseThrow().account().idUser());
        assertEquals(900L, outcome.orElseThrow().expiresInSeconds());
        assertFalse(outcome.orElseThrow().accessToken().isBlank());
    }

    @Test
    void authenticatesBeforeOpeningASession() {
        givenSuccessfulAuthentication();
        given(tokenIssuer.issue(any())).willReturn("access-token");
        given(tokenIssuer.accessTokenTtl()).willReturn(Duration.ofMinutes(15));

        flow.login("someone@example.com", "StrongPass1!", "10.0.0.1", "JUnit");

        var authentication = ArgumentCaptor.forClass(AuthenticateUserCommand.class);
        verify(authenticateUser).authenticate(authentication.capture());
        assertEquals("someone@example.com", authentication.getValue().email());

        var session = ArgumentCaptor.forClass(CreateUserSessionCommand.class);
        verify(createUserSession).create(session.capture());
        assertEquals("USR0000001", session.getValue().idUser());
    }

    @Test
    void recordsTheCallersAddressAndAgentOnTheSession() {
        givenSuccessfulAuthentication();
        given(tokenIssuer.issue(any())).willReturn("access-token");
        given(tokenIssuer.accessTokenTtl()).willReturn(Duration.ofMinutes(15));

        flow.login("someone@example.com", "StrongPass1!", "203.0.113.7", "JUnit/5");

        var session = ArgumentCaptor.forClass(CreateUserSessionCommand.class);
        verify(createUserSession).create(session.capture());
        assertEquals("203.0.113.7", session.getValue().ipAddress());
        assertEquals("JUnit/5", session.getValue().userAgent());
    }

    @Test
    void createsNoSessionAndNoTokenWhenTheAccountIsUnknown() {
        given(authenticateUser.authenticate(any())).willReturn(new AuthenticationResult(
                AuthenticationStatus.USER_NOT_FOUND, Optional.empty()));

        assertTrue(flow.login("nobody@example.com", "StrongPass1!", "10.0.0.1", "JUnit").isEmpty());

        verify(createUserSession, never()).create(any());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void createsNoSessionAndNoTokenWhenThePasswordIsWrong() {
        given(authenticateUser.authenticate(any())).willReturn(new AuthenticationResult(
                AuthenticationStatus.INVALID_CREDENTIALS, Optional.empty()));

        assertTrue(flow.login("someone@example.com", "wrong", "10.0.0.1", "JUnit").isEmpty());

        verify(createUserSession, never()).create(any());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void createsNoSessionWhenTheAccountIsBlockedOrInactive() {
        for (AuthenticationStatus blocked : new AuthenticationStatus[]{
                AuthenticationStatus.ACCOUNT_BLOCKED, AuthenticationStatus.ACCOUNT_INACTIVE}) {
            reset(authenticateUser, createUserSession, tokenIssuer, refreshTokens, secrets, identifiers);
            given(authenticateUser.authenticate(any()))
                    .willReturn(new AuthenticationResult(blocked, Optional.empty()));

            assertTrue(flow.login("someone@example.com", "StrongPass1!", "10.0.0.1", "JUnit")
                    .isEmpty(), "Status " + blocked + " must not reach the session step");
            verify(createUserSession, never()).create(any());
        }
    }

    @Test
    void returnsTheGeneratedRefreshTokenAndStoresOnlyItsHash() {
        givenSuccessfulAuthentication();
        given(tokenIssuer.issue(any())).willReturn("access-token");
        given(tokenIssuer.accessTokenTtl()).willReturn(Duration.ofMinutes(15));

        Optional<SecurityLoginFlow.LoginOutcome> outcome =
                flow.login("someone@example.com", "StrongPass1!", "10.0.0.1", "JUnit");

        assertEquals("the-refresh-token", outcome.orElseThrow().refreshToken(),
                "The client must receive the secret that was generated for it");

        var stored = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(stored.capture());
        // The flow hands the hasher the secret and stores whatever comes back, so what lands in
        // the database is the digest rather than the value the client receives. The digest's
        // own properties are covered by the hasher test and against MySQL.
        assertEquals("hash-of-the-refresh-token", stored.getValue().tokenHash());
        assertNotEquals("the-refresh-token", stored.getValue().tokenHash());
        assertEquals(RefreshTokenStatus.ACTIVE, stored.getValue().status());
    }

    @Test
    void issuesTheFirstGenerationAsTheRootOfItsFamilyAgainstTheSession() {
        givenSuccessfulAuthentication();
        given(tokenIssuer.issue(any())).willReturn("access-token");
        given(tokenIssuer.accessTokenTtl()).willReturn(Duration.ofMinutes(15));

        flow.login("someone@example.com", "StrongPass1!", "10.0.0.1", "JUnit");

        var stored = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(stored.capture());
        RefreshToken root = stored.getValue();
        // A login starts a family: the root is its own family and has no predecessor.
        assertThat(root.familyId()).isEqualTo(root.idRefreshToken());
        assertThat(root.parentId()).isEmpty();
        assertEquals("SES0000001", root.idUserSession());
    }

    private void reset(Object... mocks) {
        org.mockito.Mockito.reset(mocks);
    }
}
