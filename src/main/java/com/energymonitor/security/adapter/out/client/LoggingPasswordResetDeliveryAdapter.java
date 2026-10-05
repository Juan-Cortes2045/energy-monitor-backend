package com.energymonitor.security.adapter.out.client;

import com.energymonitor.security.application.port.out.PasswordResetDeliveryPort;
import com.energymonitor.security.domain.model.Email;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder delivery for {@link PasswordResetDeliveryPort}: it records that a secret was
 * meant to leave the server and does not send it anywhere.
 *
 * <p>This is the seam a real channel replaces. A deployment that has a mail gateway, an SMS
 * provider or a push service implements {@link PasswordResetDeliveryPort} against it, and the
 * use case, the domain and the storage stay exactly as they are. Choosing that channel, and with
 * it the message content and the reset link format, is a decision about the deployment's
 * infrastructure and not about this domain.
 *
 * <p>Until then the recovery flow is not completable end to end: a token is minted and stored,
 * but nobody receives it. That is a missing integration rather than a defect in the flow, and it
 * is recorded as such in {@code AuthController}.
 *
 * <p><strong>The secret is not logged.</strong> Not here, not in a debug line, not truncated.
 * A log is a store with a much wider audience than a password column, and writing the value
 * there would recreate exactly the exposure storing only the hash was meant to prevent. What is
 * recorded is that a delivery happened and for whom, which is what support needs and what an
 * attacker cannot redeem.
 */
@Component
public class LoggingPasswordResetDeliveryAdapter implements PasswordResetDeliveryPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingPasswordResetDeliveryAdapter.class);

    @Override
    public void deliver(Email recipient, String clearToken, Instant validUntil) {
        LOG.info("A password reset token was issued to {} and is valid until {}; no delivery "
                + "channel is configured, so it was not sent. Implement "
                + "PasswordResetDeliveryPort to deliver it.", recipient, validUntil);
    }
}
