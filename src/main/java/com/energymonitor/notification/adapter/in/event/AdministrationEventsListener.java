package com.energymonitor.notification.adapter.in.event;

import com.energymonitor.notification.application.usecase.AdminNotifications;
import com.energymonitor.security.api.AccountAdministered;
import com.energymonitor.security.api.AccountRegistered;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns platform events into administrator notifications.
 *
 * <p>After commit, so a registration or a change that rolls back never reaches an administrator,
 * and asynchronous, so the request that caused it does not wait for the rows to be written.
 */
@Component
public class AdministrationEventsListener {

    private final AdminNotifications adminNotifications;

    public AdministrationEventsListener(AdminNotifications adminNotifications) {
        this.adminNotifications = adminNotifications;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AccountRegistered event) {
        adminNotifications.record(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AccountAdministered event) {
        adminNotifications.record(event);
    }
}
