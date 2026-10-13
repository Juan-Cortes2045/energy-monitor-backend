package com.energymonitor.home.infrastructure;

import com.energymonitor.home.application.command.TransferHomeOwnershipCommand;
import com.energymonitor.home.application.port.in.TransferHomeOwnership;
import com.energymonitor.home.application.usecase.TransferHomeOwnershipService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs {@link TransferHomeOwnershipService} in one transaction.
 *
 * <p>The transfer writes two memberships: the owner becomes a MEMBER and the member becomes the
 * OWNER. Committing only one of them would leave the home with no owner or with two. The
 * transaction also holds the row locks the service takes. The service stays framework-free; this
 * decorator is the one the web layer receives ({@link Primary}).
 */
@Component
@Primary
public class TransactionalTransferHomeOwnership implements TransferHomeOwnership {

    private final TransferHomeOwnershipService target;
    private final TransactionTemplate transaction;

    public TransactionalTransferHomeOwnership(TransferHomeOwnershipService target,
                                              PlatformTransactionManager transactions) {
        this.target = target;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public void transfer(TransferHomeOwnershipCommand command) {
        transaction.executeWithoutResult(status -> target.transfer(command));
    }
}
