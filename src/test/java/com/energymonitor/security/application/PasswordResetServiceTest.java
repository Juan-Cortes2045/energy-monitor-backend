package com.energymonitor.security.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.security.application.command.CreatePasswordResetTokenCommand;
import com.energymonitor.security.application.command.ResetPasswordCommand;
import com.energymonitor.security.application.exception.InvalidResetTokenException;
import com.energymonitor.security.application.exception.PasswordPolicyViolationException;
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
 */
class PasswordResetServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

    private final UseCaseFixtures.FakeUserPersistencePort users = new UseCaseFixtures.FakeUserPersistencePort();
    private final UseCaseFixtures.FakePasswordResetTokenPersistencePort tokens =
            new UseCaseFixtures.FakePasswordResetTokenPersistencePort();
    private final UseCaseFixtures.FakeTokenGeneratorPort tokenGenerator = new UseCaseFixtures.FakeTokenGeneratorPort();
    private final UseCaseFixtures.FakeIdentifierGeneratorPort identifiers =
            new UseCaseFixtures.FakeIdentifierGeneratorPort();
    private final UseCaseFixtures.FakePasswordHasherPort hasher = new UseCaseFixtures.FakePasswordHasherPort();
    private final UseCaseFixtures.FakePasswordPolicyPersistencePort policies =
            new UseCaseFixtures.FakePasswordPolicyPersistencePort();
    private final UseCaseFixtures.FakeAuditLogPersistencePort audits =
            new UseCaseFixtures.FakeAuditLogPersistencePort();
    private final CreatePasswordResetTokenService createToken =
            new CreatePasswordResetTokenService(users, tokens, tokenGenerator, identifiers, CLOCK);
    private final ResetPasswordService resetPassword =
            new ResetPasswordService(users, tokens, hasher, policies, audits, identifiers, CLOCK);

    @Test
    void issuesAShortLivedSingleUseToken() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);

        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));

        assertTrue(token.isValid(CLOCK.instant()));
        assertEquals(CLOCK.instant().plus(Duration.ofMinutes(30)), token.expirationAt());
    }

    @Test
    void redeemingValidTokenReplacesTheHashAndConsumesIt() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));

        resetPassword.reset(new ResetPasswordCommand(token.resetToken(), "NewStrong1!", "10.0.0.4"));

        assertTrue(hasher.matches("NewStrong1!", users.findActive("use0000001").orElseThrow().passwordHash()));
        assertTrue(tokens.findActiveByValue(token.resetToken()).orElseThrow().isUsed());
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
        tokens.store(new PasswordResetToken("tok0000001", "use0000001", "stale-token",
                now.minus(Duration.ofHours(1)), now.minus(Duration.ofMinutes(30)), false));

        assertThrows(InvalidResetTokenException.class,
                () -> resetPassword.reset(new ResetPasswordCommand("stale-token", "NewStrong1!", null)));
    }

    @Test
    void abandonsTheAccountWhenTheTokenWasAlreadyUsed() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        Instant now = CLOCK.instant();
        tokens.store(new PasswordResetToken("tok0000001", "use0000001", "used-token",
                now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofMinutes(20)), true));

        assertThrows(InvalidResetTokenException.class,
                () -> resetPassword.reset(new ResetPasswordCommand("used-token", "NewStrong1!", null)));
    }

    @Test
    void rejectsNewPasswordBelowPolicyBeforeConsumingTheToken() {
        users.seed("use0000001", "per0000001", "ada@example.com", UserStatus.ACTIVE);
        PasswordResetToken token = createToken.create(new CreatePasswordResetTokenCommand("ada@example.com"));

        assertThrows(PasswordPolicyViolationException.class,
                () -> resetPassword.reset(new ResetPasswordCommand(token.resetToken(), "weak", null)));

        assertTrue(tokens.findActiveByValue(token.resetToken()).orElseThrow().isValid(CLOCK.instant()));
    }
}