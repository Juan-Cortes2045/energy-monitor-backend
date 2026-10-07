package com.energymonitor.notification.adapter.out.client;

import com.energymonitor.notification.application.port.out.NotificationIdentifierPort;
import com.energymonitor.notification.application.usecase.SendEmailVerification;
import com.energymonitor.notification.infrastructure.mail.EmailSender;
import com.energymonitor.security.api.EmailVerificationDeliveryPort;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Delivers email-verification codes by mail, the same way {@link EmailPasswordResetDeliveryAdapter}
 * delivers recovery codes: the delivery record is written on the calling thread, and the SMTP
 * exchange runs on the shared bounded executor once the caller's transaction, if any, commits.
 */
@Component
public class EmailVerificationDeliveryAdapter implements EmailVerificationDeliveryPort {

    private final SendEmailVerification sendEmailVerification;
    private final NotificationIdentifierPort identifiers;
    private final ThreadPoolExecutor executor;

    public EmailVerificationDeliveryAdapter(SendEmailVerification sendEmailVerification,
            NotificationIdentifierPort identifiers,
            @Qualifier("passwordResetExecutor") ThreadPoolExecutor executor) {
        this.sendEmailVerification = sendEmailVerification;
        this.identifiers = identifiers;
        this.executor = executor;
    }

    @Override
    public void deliver(String userId, String recipient, String clearCode, Instant validUntil) {
        EmailSender.PreparedDelivery pending = sendEmailVerification.openForDelivery(
                identifiers.generate(), userId, recipient, clearCode, validUntil);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueue(pending);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueue(pending);
            }
        });
    }

    private void enqueue(EmailSender.PreparedDelivery pending) {
        try {
            executor.execute(() -> sendEmailVerification.send(pending));
        } catch (RejectedExecutionException refused) {
            sendEmailVerification.abandon(pending,
                    "the delivery queue is full, so the message was not attempted");
        }
    }
}
