package com.energymonitor.security.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.security.application.command.CreatePasswordResetTokenCommand;
import com.energymonitor.security.application.command.ResetPasswordCommand;
import com.energymonitor.security.application.exception.InvalidResetTokenException;
import com.energymonitor.security.application.exception.PasswordPolicyViolationException;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.usecase.CreatePasswordResetTokenService;
import com.energymonitor.security.application.usecase.ResetPasswordService;
import com.energymonitor.security.domain.model.AuditAction;
import com.energymonitor.security.domain.model.PasswordResetToken;
import com.energymonitor.security.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * {@code CreatePasswordResetToken} and {@code ResetPassword} requirements: a token is valid
 * only once and only within its window, redeeming one replaces the hash, consumes it and
 * audits the change.
 *
 * <p>The secret that leaves the use case and the hash that stays behind are asserted separately,
 * because the whole point of the flow is that they are two different values: what is delivered
 * is usable, what is stored is not.
 */
class PasswordResetServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

    private final UseCaseFixtures.FakeUserPersistencePort users = new UseCaseFixtures.FakeUserPersistencePort();
    private final UseCaseFixtures.FakePasswordResetTokenPersistencePort tokens =
            new UseCaseFixtures.FakePasswordResetTokenPersistencePort();
    private final UseCaseFixtures.FakeTokenGeneratorPort tokenGenerator = new UseCaseFixtures.FakeTokenGeneratorPort();
    private final UseCaseFixtures.FakePasswordResetTokenHasherPort tokenHasher =
            new UseCaseFixtures.FakePasswordResetTokenHasherPort();
    private final UseCaseFixtures.FakePasswordResetDeliveryPort delivery =
            new UseCaseFixtures.FakePasswordResetDeliveryPort();
    private final UseCaseFixtures.FakeIdentifierGeneratorPort identifiers =
            new UseCaseFixtures.FakeIdentifierGeneratorPort();
    private final UseCaseFixtures.FakePasswordHasherPort hasher = new UseCaseFixtures.FakePasswordHasherPort();
    private final UseCaseFixtures.FakePasswordPolicyPersistencePort policies =
            new UseCaseFixtures.FakePasswordPolicyPersistencePort();
    private final UseCaseFixtures.FakeAuditLogPersistencePort audits =
            new UseCaseFixtures.FakeAuditLogPersistencePort();
    private final CreatePasswordResetTokenService createToken =
            new CreatePasswordResetTokenService(users, tokens, tokenGenerator, tokenHasher,
                    delivery, identifiers, CLOCK);
    private final ResetPasswordService resetPassword =
            new ResetPasswordService(users, tokens, tokenHasher, hasher, policies, audits,
                    identifiers, CLOCK);

    @Test
    void issuesAShortLivedSingleUseToken() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);

        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));

        assertTrue(token.isValid(CLOCK.instant()));
        assertEquals(CLOCK.instant().plus(Duration.ofMinutes(30)), token.expirationAt());
    }

    @Test
    void storesTheHashAndDeliversTheSecret() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);

        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));

        String clearToken = token.clearToken().orElseThrow();
        assertNotEquals(clearToken, token.resetTokenHash(), "The stored value must not be the secret");
        // What the store is keyed by is the hash, and the delivered value is what redeems it.
        assertEquals(token, tokens.findActiveByHash(tokenHasher.hash(clearToken)).orElseThrow());
        var sent = delivery.deliveries().getFirst();
        assertEquals(clearToken, sent.clearToken());
        assertEquals("ada@example.com", sent.recipient().value());
        assertEquals(token.expirationAt(), sent.validUntil());
    }

    @Test
    void aSecretIsNeverDeliveredToAnUnknownAccount() {
        // Nothing is minted, so nothing can be delivered: that is what keeps the acknowledgement
        // outside the controller from meaning anything about registration.
        assertThrows(UserNotFoundException.class,
                () -> createToken.create(new CreatePasswordResetTokenCommand("nobody@example.com")));

        assertTrue(delivery.deliveries().isEmpty());
    }

    @Test
    void anUndeliverableChannelDoesNotFailTheRequest() {
        // The endpoint answers 202 either way; a transport failure escaping here would make the
        // difference observable and turn an outage into an account-enumeration oracle.
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        delivery.failWith(new IllegalStateException("mail gateway unreachable"));

        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));

        assertTrue(token.isValid(CLOCK.instant()));
        assertEquals(token, tokens.findActiveByHash(token.resetTokenHash()).orElseThrow());
    }

    @Test
    void redeemingValidTokenReplacesTheHashAndConsumesIt() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));
        String clearToken = token.clearToken().orElseThrow();

        resetPassword.reset(new ResetPasswordCommand(clearToken, "NewStrong1!", "10.0.0.4"));

        assertTrue(hasher.matches("NewStrong1!", users.findActive("use0000001").orElseThrow().passwordHash()));
        assertTrue(tokens.findActiveByHash(tokenHasher.hash(clearToken)).orElseThrow().isUsed());
        assertEquals(AuditAction.UPDATE, audits.logs().getFirst().action());
    }

    @Test
    void abandonsTheAccountWhenTheTokenIsUnknown() {
        assertThrows(InvalidResetTokenException.class,
                () -> resetPassword.reset(new ResetPasswordCommand("ghost", "NewStrong1!", null)));
    }

    @Test
    void abandonsTheAccountWhenTheTokenHasExpired() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        Instant now = CLOCK.instant();
        tokens.store(PasswordResetToken.rehydrate("tok0000001", "use0000001",
                tokenHasher.hash("stale-token"), now.minus(Duration.ofHours(1)),
                now.minus(Duration.ofMinutes(30)), false));

        assertThrows(InvalidResetTokenException.class,
                () -> resetPassword.reset(new ResetPasswordCommand("stale-token", "NewStrong1!", null)));
    }

    @Test
    void abandonsTheAccountWhenTheTokenWasAlreadyUsed() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        Instant now = CLOCK.instant();
        tokens.store(PasswordResetToken.rehydrate("tok0000001", "use0000001",
                tokenHasher.hash("used-token"), now.minus(Duration.ofMinutes(10)),
                now.plus(Duration.ofMinutes(20)), true));

        assertThrows(InvalidResetTokenException.class,
                () -> resetPassword.reset(new ResetPasswordCommand("used-token", "NewStrong1!", null)));
    }

    @Test
    void rejectsNewPasswordBelowPolicyBeforeConsumingTheToken() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));
        String clearToken = token.clearToken().orElseThrow();

        assertThrows(PasswordPolicyViolationException.class,
                () -> resetPassword.reset(new ResetPasswordCommand(clearToken, "weak", null)));

        assertTrue(tokens.findActiveByHash(tokenHasher.hash(clearToken)).orElseThrow()
                .isValid(CLOCK.instant()));
    }
}
