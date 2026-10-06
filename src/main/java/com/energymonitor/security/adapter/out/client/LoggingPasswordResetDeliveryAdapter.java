package com.energymonitor.security.adapter.out.client;

import com.energymonitor.security.api.PasswordResetDeliveryPort;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder delivery for {@link PasswordResetDeliveryPort}: it records that a code was meant to
 * leave the server and sends it nowhere.
 *
 * <p>This is the seam a real channel replaces. The module that actually sends the mail implements
 * this same interface, and the use case, the domain and the storage stay exactly as they are.
 *
 * <p>Until then the recovery flow is not completable end to end: a code is minted and stored, but
 * nobody receives it. That is a missing integration rather than a defect in the flow.
 *
 * <p><strong>The code is not logged.</strong> Not here, not in a debug line, not truncated to its
 * last digits. A log is a store with a much wider audience than the token table, and two surviving
 * digits would still cut a million candidates down to ten thousand. What is recorded is that a
 * delivery happened, for whom and until when, which is what support needs and what an attacker
 * cannot redeem.
 */
@Component
public class LoggingPasswordResetDeliveryAdapter implements PasswordResetDeliveryPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingPasswordResetDeliveryAdapter.class);

    @Override
    public void deliver(String userId, String recipient, String idResetToken, String clearToken,
                        Instant validUntil) {
        LOG.info("A password reset code was issued for user {} to {} and is valid until {}; no "
                + "delivery channel is configured, so it was not sent. The code itself is not "
                + "recorded anywhere.", userId, recipient, validUntil);
    }
}