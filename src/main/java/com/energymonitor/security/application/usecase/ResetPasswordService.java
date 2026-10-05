package com.energymonitor.security.application.usecase;

import com.energymonitor.security.application.command.ResetPasswordCommand;
import com.energymonitor.security.application.exception.InvalidResetTokenException;
import com.energymonitor.security.application.exception.PasswordPolicyViolationException;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.ResetPassword;
import com.energymonitor.security.application.port.out.AuditLogPersistencePort;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.PasswordPolicyPersistencePort;
import com.energymonitor.security.application.port.out.PasswordResetTokenHasherPort;
import com.energymonitor.security.application.port.out.PasswordResetTokenPersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.domain.model.AuditAction;
import com.energymonitor.security.domain.model.AuditLog;
import com.energymonitor.security.domain.model.PasswordPolicy;
import com.energymonitor.security.domain.model.PasswordResetToken;
import com.energymonitor.security.domain.model.User;
import java.time.Clock;
import java.time.Instant;

/**
 * Redeems a password-reset token and replaces the account password.
 *
 * <p>The caller presents the secret it received through the reset channel; the store holds only
 * its hash. Resolving one from the other is the first thing this does, and every rejection after
 * that is the same {@link InvalidResetTokenException}: an unknown hash, an expired window and an
 * already consumed token are indistinguishable from out here, so the endpoint cannot be used to
 * probe which of the three happened.
 */
public class ResetPasswordService implements ResetPassword {

    private final UserPersistencePort userPort;
    private final PasswordResetTokenPersistencePort tokenPort;
    private final PasswordResetTokenHasherPort hasher;
    private final PasswordHasherPort passwordHasher;
    private final PasswordPolicyPersistencePort passwordPolicyPort;
    private final AuditLogPersistencePort auditLogPort;
    private final IdentifierGeneratorPort identifiers;
    private final Clock clock;

    public ResetPasswordService(UserPersistencePort userPort,
                                PasswordResetTokenPersistencePort tokenPort,
                                PasswordResetTokenHasherPort hasher,
                                PasswordHasherPort passwordHasher,
                                PasswordPolicyPersistencePort passwordPolicyPort,
                                AuditLogPersistencePort auditLogPort,
                                IdentifierGeneratorPort identifiers, Clock clock) {
        this.userPort = userPort;
        this.tokenPort = tokenPort;
        this.hasher = hasher;
        this.passwordHasher = passwordHasher;
        this.passwordPolicyPort = passwordPolicyPort;
        this.auditLogPort = auditLogPort;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    @Override
    public void reset(ResetPasswordCommand command) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenPort.findActiveByHash(hasher.hash(command.resetToken()))
                .orElseThrow(() -> new InvalidResetTokenException("unknown or inactive reset token"));
        if (!token.isValid(now)) {
            throw new InvalidResetTokenException("reset token is expired or already used");
        }
        PasswordPolicy policy = passwordPolicyPort.activePolicy()
                .orElseThrow(() -> new IllegalStateException("no active password policy is configured"));
        if (!policy.isSatisfiedBy(command.newPassword())) {
            throw new PasswordPolicyViolationException("password does not satisfy the configured policy");
        }
        User user = userPort.findActive(token.idUser())
                .orElseThrow(() -> new UserNotFoundException("no active user " + token.idUser()));

        user.changePassword(passwordHasher.hash(command.newPassword()));
        userPort.save(user);
        token.markUsed();
        tokenPort.update(token);
        auditLogPort.save(new AuditLog(identifiers.generate(), user.idUser(), AuditAction.UPDATE,
                null, command.ipAddress(), null, now));
    }
}