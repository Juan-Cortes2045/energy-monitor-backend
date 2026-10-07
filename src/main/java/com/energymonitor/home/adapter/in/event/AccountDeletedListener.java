package com.energymonitor.home.adapter.in.event;

import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.security.api.AccountDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to an account deletion in the security module. Synchronous on purpose: it runs in the
 * deleting transaction, so both modules commit or roll back together.
 */
@Component
public class AccountDeletedListener {

    private final RemoveDeletedAccount removeDeletedAccount;

    public AccountDeletedListener(RemoveDeletedAccount removeDeletedAccount) {
        this.removeDeletedAccount = removeDeletedAccount;
    }

    @EventListener
    public void on(AccountDeleted event) {
        removeDeletedAccount.remove(event.userId());
    }
}
