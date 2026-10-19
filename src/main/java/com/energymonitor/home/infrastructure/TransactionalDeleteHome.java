package com.energymonitor.home.infrastructure;

import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.port.in.DeleteHome;
import com.energymonitor.home.application.usecase.DeleteHomeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs {@link DeleteHomeService} in one transaction.
 *
 * <p>Deleting a home writes memberships, thresholds and the home, and the device module unlinks
 * the home's devices in the same call (synchronous listener of {@code HomeDeleted}). Half of that
 * must never stay committed: a home without memberships but with linked devices would keep
 * accepting readings nobody can see. The service stays framework-free; this decorator is the one
 * the web layer receives ({@link Primary}).
 */
@Component
@Primary
public class TransactionalDeleteHome implements DeleteHome {

    private final DeleteHomeService target;
    private final TransactionTemplate transaction;

    public TransactionalDeleteHome(DeleteHomeService target, PlatformTransactionManager transactions) {
        this.target = target;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public void delete(DeleteHomeCommand command) {
        transaction.executeWithoutResult(status -> target.delete(command));
    }
}
