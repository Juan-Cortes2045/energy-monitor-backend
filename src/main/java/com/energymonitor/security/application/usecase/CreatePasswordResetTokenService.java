package com.energymonitor.security.application.usecase;

import com.energymonitor.security.application.command.CreatePasswordResetTokenCommand;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.CreatePasswordResetToken;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.PasswordResetDeliveryPort;
import com.energymonitor.security.application.port.out.PasswordResetTokenHasherPort;
import com.energymonitor.security.application.port.out.PasswordResetTokenPersistencePort;
import com.energymonitor.security.application.port.out.TokenGeneratorPort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.PasswordResetToken;
import com.energymonitor.security.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Issues a short-lived, single-use password-recovery token and puts it on its way to its owner.
 *
 * <h2>The secret exists twice, and only ever as a hash in between</h2>
 *
 * <p>A clear token is generated here, hashed immediately, and stored as the hash. The clear
 * value leaves this method exactly once, through {@link PasswordResetDeliveryPort}, which is the
 * only channel entitled to carry it. Nothing here returns it to a caller that could serialise
 * it: the use case does hand the token object back to the web adapter, and that object still
 * carries the secret in memory, but no response DTO is built from it and no field of it is
 * persisted.
 *
 * <h2>Why delivery failures do not fail the request</h2>
 *
 * <p>The endpoint that triggers this answers 202 whether or not the account exists. A delivery
 * adapter that let its transport failure escape would therefore change the answer for exactly
 * the addresses that are registered, turning an outage into a way of enumerating them. The
 * failure is logged and swallowed instead: the stored token stays valid, so support can still
 * act on it, and the caller learns nothing beyond the acknowledgement it always gets.
 */
public class CreatePasswordResetTokenService implements CreatePasswordResetToken {

    private static final Logger LOG =
            LoggerFactory.getLogger(CreatePasswordResetTokenService.class);

    private static final Duration VALIDITY = Duration.ofMinutes(30);

    private final UserPersistencePort userPort;
    private final PasswordResetTokenPersistencePort tokenPort;
    private final TokenGeneratorPort tokenGenerator;
    private final PasswordResetTokenHasherPort hasher;
    private final PasswordResetDeliveryPort delivery;
    private final IdentifierGeneratorPort identifiers;
    private final Clock clock;

    public CreatePasswordResetTokenService(UserPersistencePort userPort,
                                           PasswordResetTokenPersistencePort tokenPort,
                                           TokenGeneratorPort tokenGenerator,
                                           PasswordResetTokenHasherPort hasher,
                                           PasswordResetDeliveryPort delivery,
                                           IdentifierGeneratorPort identifiers, Clock clock) {
        this.userPort = userPort;
        this.tokenPort = tokenPort;
        this.tokenGenerator = tokenGenerator;
        this.hasher = hasher;
        this.delivery = delivery;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    @Override
    public PasswordResetToken create(CreatePasswordResetTokenCommand command) {
        Instant now = clock.instant();
        User user = userPort.findActiveByEmail(Email.of(command.email()))
                .orElseThrow(() -> new UserNotFoundException("no active account for " + command.email()));
        String clearToken = tokenGenerator.generateToken();
        PasswordResetToken token = PasswordResetToken.issue(identifiers.generate(), user.idUser(),
                clearToken, hasher.hash(clearToken), now, now.plus(VALIDITY));
        PasswordResetToken stored = tokenPort.save(token);
        deliver(user.email(), stored);
        return stored;
    }

    /**
     * Hands the secret to the delivery channel, if the token still carries it.
     *
     * @param recipient the account that asked for the reset
     * @param token     the stored token, holding the clear secret in memory
     */
    private void deliver(Email recipient, PasswordResetToken token) {
        String clearToken = token.clearToken().orElse(null);
        if (clearToken == null) {
            // The token came back from the store without a secret, so there is nothing to send.
            // Persisting and delivering are separate concerns and this must not fail the flow.
            LOG.warn("The stored password reset token {} carries no secret to deliver",
                    token.idResetToken());
            return;
        }
        try {
            delivery.deliver(recipient, clearToken, token.expirationAt());
        } catch (RuntimeException undeliverable) {
            // The token identifier, not the address: the row already says whose it is, and an
            // address in a log is personal data with a much longer life than this failure has.
            LOG.warn("The password reset token {} could not be delivered", token.idResetToken(),
                    undeliverable);
        }
    }
}
