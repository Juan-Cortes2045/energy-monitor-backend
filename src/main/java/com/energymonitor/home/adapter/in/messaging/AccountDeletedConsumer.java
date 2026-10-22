package com.energymonitor.home.adapter.in.messaging;

import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.home.infrastructure.HomeMessagingConfiguration;
import java.time.Instant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Removes the memberships of an account the security service deleted, and the homes it was the
 * only owner of.
 *
 * <p>The deletion is already committed when the message arrives, so the cleanup runs in a
 * transaction of its own and is idempotent: a second delivery finds no membership left and does
 * nothing. A home the account was the only owner of, with other members left, passes to the
 * oldest of them instead of blocking the message.
 */
@Component
public class AccountDeletedConsumer {

    private final RemoveDeletedAccount removeDeletedAccount;
    private final TransactionTemplate transaction;
    private final ObjectMapper json;

    public AccountDeletedConsumer(RemoveDeletedAccount removeDeletedAccount,
                                  PlatformTransactionManager transactions, ObjectMapper json) {
        this.removeDeletedAccount = removeDeletedAccount;
        this.transaction = new TransactionTemplate(transactions);
        this.json = json;
    }

    @RabbitListener(queues = HomeMessagingConfiguration.ACCOUNT_DELETED_QUEUE)
    public void on(Message message) {
        var event = json.readValue(message.getBody(), AccountDeletedMessage.class);
        transaction.executeWithoutResult(status -> removeDeletedAccount.remove(event.userId()));
    }

    /** Body of {@code account.deleted}. */
    record AccountDeletedMessage(String userId, Instant deletedAt) {
    }
}
