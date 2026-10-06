package com.energymonitor.notification.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.energymonitor.notification.application.port.out.NotificationIdentifierPort;
import com.energymonitor.notification.application.usecase.SendPasswordResetEmail;
import com.energymonitor.notification.infrastructure.mail.EmailSender;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * When the delivery is allowed to run.
 *
 * <p>The two properties that matter are both about ordering against a transaction, and neither is
 * observable from the return value: that a rollback sends nothing, and that a commit sends without
 * the request waiting for it. Both are asserted through the gate, because a send queued from the
 * wrong side of the commit is exactly the bug these catch.
 *
 * <p>The executor is real in every case except the rejection, where a stub that refuses is the point.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Email delivery of a recovery code")
class EmailPasswordResetDeliveryAdapterTest {

    private static final String ID = "ntf0000001";
    private static final String USER = "use0000001";
    private static final String SOURCE = "rst0000001";
    private static final String RECIPIENT = "ada@example.com";
    private static final String CODE = "795024";
    private static final Instant VALID_UNTIL = Instant.parse("2026-01-02T03:34:05Z");

    @Mock
    private SendPasswordResetEmail sendPasswordResetEmail;

    @Mock
    private NotificationIdentifierPort identifiers;

    private EmailSender.PreparedDelivery pending() {
        return new EmailSender.PreparedDelivery(ID, RECIPIENT, "Recupera tu contraseña",
                "Tu código es: " + CODE, "<p>" + CODE + "</p>");
    }

    private void givenAPreparedDelivery() {
        // any() and not anyString() for the first argument: the identifier comes from a generator,
        // and a null from an unstubbed mock does not match a string matcher.
        // any() wherever the argument can be null: the generated identifier and the first name,
        // which this port does not carry, both arrive absent, and a string matcher rejects null.
        doAnswer(invocation -> pending()).when(sendPasswordResetEmail).openForDelivery(any(),
                anyString(), any(), anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    @DisplayName("nothing is sent when the issuing transaction rolls back")
    void rollbackSendsNothing() {
        givenAPreparedDelivery();
        RecordingExecutor recording = new RecordingExecutor();
        EmailPasswordResetDeliveryAdapter adapter =
                new EmailPasswordResetDeliveryAdapter(sendPasswordResetEmail, identifiers, recording);

        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.deliver(USER, RECIPIENT, SOURCE, CODE, VALID_UNTIL);
            // The record exists already: this is the write that survives, and it is deliberately not
            // what this test is about.
            verify(sendPasswordResetEmail).openForDelivery(any(), anyString(), any(),
                anyString(), anyString(), any(), anyString(), any());

            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(recording.dispatched)
                .as("a rollback must not produce a message carrying a code that was never stored")
                .isZero();
    }

    @Test
    @DisplayName("the send is queued only after the transaction commits")
    void commitQueuesTheSend() {
        givenAPreparedDelivery();
        RecordingExecutor recording = new RecordingExecutor();
        EmailPasswordResetDeliveryAdapter adapter =
                new EmailPasswordResetDeliveryAdapter(sendPasswordResetEmail, identifiers, recording);

        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.deliver(USER, RECIPIENT, SOURCE, CODE, VALID_UNTIL);

            assertThat(recording.dispatched)
                    .as("nothing may be queued before the commit")
                    .isZero();

            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCommit());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(recording.dispatched).isEqualTo(1);
        recording.runAll();
        verify(sendPasswordResetEmail).send(any(EmailSender.PreparedDelivery.class));
    }

    @Test
    @DisplayName("the request does not wait for the send")
    void theRequestDoesNotWaitForTheSend() {
        givenAPreparedDelivery();
        // A queue that records but never runs. If deliver() waited for the send to finish, this test
        // would hang instead of failing, which is the point: the return of the call is the thing
        // being asserted.
        RecordingExecutor recording = new RecordingExecutor();
        EmailPasswordResetDeliveryAdapter adapter =
                new EmailPasswordResetDeliveryAdapter(sendPasswordResetEmail, identifiers, recording);

        adapter.deliver(USER, RECIPIENT, SOURCE, CODE, VALID_UNTIL);

        assertThat(recording.dispatched).isEqualTo(1);
        assertThat(recording.completed)
                .as("deliver() returned before the send ran")
                .isZero();
        verify(sendPasswordResetEmail, never()).send(any());
    }

    @Test
    @DisplayName("a queue that refuses the work closes the record as failed")
    void aRefusedQueueClosesTheRecordAsFailed() {
        givenAPreparedDelivery();
        EmailPasswordResetDeliveryAdapter adapter =
                new EmailPasswordResetDeliveryAdapter(sendPasswordResetEmail, identifiers, refusing());

        adapter.deliver(USER, RECIPIENT, SOURCE, CODE, VALID_UNTIL);

        verify(sendPasswordResetEmail)
                .abandon(any(EmailSender.PreparedDelivery.class), anyString());
        verify(sendPasswordResetEmail, never()).send(any());
    }

    @Test
    @DisplayName("without a transaction the send is queued straight away")
    void withoutATransactionTheSendIsQueuedImmediately() {
        givenAPreparedDelivery();
        RecordingExecutor recording = new RecordingExecutor();
        EmailPasswordResetDeliveryAdapter adapter =
                new EmailPasswordResetDeliveryAdapter(sendPasswordResetEmail, identifiers, recording);

        adapter.deliver(USER, RECIPIENT, SOURCE, CODE, VALID_UNTIL);

        // Nothing can roll back a row written with no transaction around it, so waiting would buy
        // nothing and the request would be held for no reason.
        assertThat(recording.dispatched).isEqualTo(1);
    }

    /**
     * A pool that refuses every submission, which is what a full bounded queue does.
     *
     * <p>Modelled rather than filled deliberately: the property under test is how the adapter
     * reacts to a refusal, and a queue filled by timing would make that assertion depend on
     * scheduling.
     */
    private ThreadPoolExecutor refusing() {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()) {
            @Override
            public void execute(Runnable command) {
                throw new RejectedExecutionException("delivery queue is full");
            }
        };
    }

    /** Records what was handed to the executor and runs it only when the test says so. */
    private static final class RecordingExecutor extends ThreadPoolExecutor {

        private final List<Runnable> captured = new ArrayList<>();
        private volatile int dispatched;
        private volatile int completed;

        private RecordingExecutor() {
            super(0, 1, 0L, TimeUnit.MILLISECONDS,
                    new java.util.concurrent.LinkedBlockingQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy());
            allowCoreThreadTimeOut(false);
        }

        @Override
        public void execute(Runnable command) {
            captured.add(command);
            dispatched++;
        }

        private void runAll() {
            for (Runnable command : captured) {
                command.run();
                completed++;
            }
        }
    }
}