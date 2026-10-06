package com.energymonitor.notification.adapter.out.client;

import com.energymonitor.notification.application.port.out.NotificationIdentifierPort;
import com.energymonitor.notification.application.usecase.SendPasswordResetEmail;
import com.energymonitor.notification.infrastructure.mail.EmailSender;
import com.energymonitor.security.api.PasswordResetDeliveryPort;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Delivers a password recovery secret by email, off the request thread and only after the issuing
 * transaction commits.
 *
 * <p>This is the adapter the {@link PasswordResetDeliveryPort} Javadoc describes as the seam a real
 * channel fills, and it replaces the logging placeholder that stood there while no channel was
 * configured.
 *
 * <p><strong>The direction of the dependency is the point.</strong> The security module declares the
 * port and knows nothing about mail, SMTP, queues or message wording; this class is what makes the
 * two meet. Choosing a different channel replaces this file and leaves the recovery flow, the
 * hashing and the storage untouched.
 *
 * <h2>Nothing leaves before the code is on file</h2>
 *
 * <p>The request writes the recovery code and this class is called from the same transaction. A mail
 * server answered before that transaction committed would produce the worst of both outcomes on a
 * rollback: a message carrying a code that does not exist, to an address that cannot use it. The
 * send is therefore registered as an after-commit callback and runs only once the row is durable.
 *
 * <h2>Nothing waits for the mail server either</h2>
 *
 * <p>The recovery endpoint answers 202 whether or not the account exists, so it must not wait on an
 * SMTP conversation or fail because of one. The record is written synchronously and the send is
 * queued. When the queue refuses the work, the record is closed as failed here rather than left
 * claiming a delivery that will not happen.
 *
 * <p><strong>The first name is not available here.</strong> The port hands over an address, a secret
 * and an expiry, and nothing else, so the greeting falls back to a neutral form rather than this
 * adapter reaching back into the account to look one up. Widening the port is a change to the
 * security module's contract, not something to work around from this side.
 */
@Component
public class EmailPasswordResetDeliveryAdapter implements PasswordResetDeliveryPort {

    private final SendPasswordResetEmail sendPasswordResetEmail;
    private final NotificationIdentifierPort identifiers;
    private final ThreadPoolExecutor executor;

    public EmailPasswordResetDeliveryAdapter(SendPasswordResetEmail sendPasswordResetEmail,
            NotificationIdentifierPort identifiers,
            @Qualifier("passwordResetExecutor") ThreadPoolExecutor executor) {
        this.sendPasswordResetEmail = sendPasswordResetEmail;
        this.identifiers = identifiers;
        this.executor = executor;
    }

    @Override
    public void deliver(String userId, String recipient, String idResetToken, String clearToken,
            Instant validUntil) {
        // On the request thread: this is the write that leaves evidence of a delivery, and it must
        // exist even if the send is never attempted.
        EmailSender.PreparedDelivery pending = sendPasswordResetEmail.openForDelivery(
                identifiers.generate(), userId, null, idResetToken, recipient, null, clearToken,
                validUntil);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No transaction to wait for, which happens when a caller drives this port directly. The
            // record is already written and nothing can roll it back, so waiting buys nothing.
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

    /**
     * Hands the send to the queue, closing the record as failed if the queue refuses it.
     *
     * <p>A refusal means the queue is full, which is the queue doing its job. It is reported on the
     * record and never propagated: the caller is a request that must answer 202 regardless, and an
     * exception here would be exactly the difference between registered and unregistered addresses
     * that the endpoint exists to hide.
     */
    private void enqueue(EmailSender.PreparedDelivery pending) {
        try {
            executor.execute(() -> sendPasswordResetEmail.send(pending));
        } catch (RejectedExecutionException refused) {
            sendPasswordResetEmail.abandon(pending,
                    "the delivery queue is full, so the message was not attempted");
        }
    }
}