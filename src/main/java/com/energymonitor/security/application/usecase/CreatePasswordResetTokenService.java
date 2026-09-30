package com.energymonitor.security.application.usecase;

import com.energymonitor.security.application.command.CreatePasswordResetTokenCommand;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.CreatePasswordResetToken;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.PasswordResetTokenPersistencePort;
import com.energymonitor.security.application.port.out.TokenGeneratorPort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.PasswordResetToken;
import com.energymonitor.security.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues a short-lived, single-use password-recovery token.
 */
public class CreatePasswordResetTokenService implements CreatePasswordResetToken {

    private static final Duration VALIDITY = Duration.ofMinutes(30);

    private final UserPersistencePort userPort;
    private final PasswordResetTokenPersistencePort tokenPort;
    private final TokenGeneratorPort tokenGenerator;
    private final IdentifierGeneratorPort identifiers;
    private final Clock clock;

    public CreatePasswordResetTokenService(UserPersistencePort userPort,
                                           PasswordResetTokenPersistencePort tokenPort,
                                           TokenGeneratorPort tokenGenerator,
                                           IdentifierGeneratorPort identifiers, Clock clock) {
        this.userPort = userPort;
        this.tokenPort = tokenPort;
        this.tokenGenerator = tokenGenerator;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    @Override
    public PasswordResetToken create(CreatePasswordResetTokenCommand command) {
        Instant now = clock.instant();
        User user = userPort.findActiveByEmail(Email.of(command.email()))
                .orElseThrow(() -> new UserNotFoundException("no active account for " + command.email()));
        PasswordResetToken token = new PasswordResetToken(identifiers.generate(), user.idUser(),
                tokenGenerator.generateToken(), now, now.plus(VALIDITY));
        return tokenPort.save(token);
    }
}