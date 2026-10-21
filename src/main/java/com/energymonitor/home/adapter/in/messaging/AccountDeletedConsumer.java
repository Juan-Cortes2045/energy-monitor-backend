package com.energymonitor.home.adapter.in.messaging;

import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.home.infrastructure.HomeMessagingConfiguration;
import com.energymonitor.security.api.AccountDeletionBlockedException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
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
 * <p>Inside the monolith this ran in the deleting transaction, through
 * {@code AccountDeletedListener}. Across services the deletion is already committed when the
 * message arrives, so the cleanup runs in a transaction of its own and is idempotent: a second
 * delivery finds no membership left and does nothing.
 *
 * <p>A cleanup the home rules refuse (the account is the only owner of a home that still has
 * other members) cannot undo the deletion any more; the message goes to the dead-letter queue
 * for a person to resolve.
 */
@Component
public class AccountDeletedConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AccountDeletedConsumer.class);

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
        try {
            transaction.executeWithoutResult(status -> removeDeletedAccount.remove(event.userId()));
        } catch (AccountDeletionBlockedException blocked) {
            LOG.error("Account {} was deleted but still owns a home with other members; "
                    + "the message goes to the dead-letter queue", event.userId());
            throw new AmqpRejectAndDontRequeueException(blocked.getMessage(), blocked);
        }
    }

    /** Body of {@code account.deleted}. */
    record AccountDeletedMessage(String userId, Instant deletedAt) {
    }
}
