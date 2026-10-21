package com.energymonitor.notification.adapter.in.messaging;

import com.energymonitor.notification.application.usecase.AdminNotifications;
import com.energymonitor.notification.infrastructure.NotificationMessagingConfiguration;
import com.energymonitor.security.api.AccountAdministered;
import com.energymonitor.security.api.AccountRegistered;
import java.time.Instant;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns the account events of the security service into in-app notifications for the
 * administrators.
 *
 * <p>The broker may deliver an event twice; {@link AdminNotifications} writes it once.
 */
@Component
public class AccountEventsConsumer {

    static final String ACCOUNT_REGISTERED = "account.registered";
    static final String ACCOUNT_ADMINISTERED = "account.administered";

    private final AdminNotifications adminNotifications;
    private final ObjectMapper json;

    public AccountEventsConsumer(AdminNotifications adminNotifications, ObjectMapper json) {
        this.adminNotifications = adminNotifications;
        this.json = json;
    }

    @RabbitListener(queues = NotificationMessagingConfiguration.ACCOUNT_EVENTS_QUEUE)
    public void on(Message message) {
        String type = message.getMessageProperties().getType();
        byte[] body = message.getBody();
        if (ACCOUNT_REGISTERED.equals(type)) {
            var event = json.readValue(body, AccountRegisteredMessage.class);
            adminNotifications.record(
                    new AccountRegistered(event.userId(), event.email(), event.occurredAt()));
        } else if (ACCOUNT_ADMINISTERED.equals(type)) {
            var event = json.readValue(body, AccountAdministeredMessage.class);
            adminNotifications.record(new AccountAdministered(event.actorId(), event.userId(),
                    AccountAdministered.Action.valueOf(event.action()), event.detail(),
                    event.occurredAt()));
        } else {
            throw new AmqpRejectAndDontRequeueException("unexpected message type " + type);
        }
    }

    /** Body of {@code account.registered}. */
    record AccountRegisteredMessage(String userId, String email, Instant occurredAt) {
    }

    /** Body of {@code account.administered}. */
    record AccountAdministeredMessage(String actorId, String userId, String action, String detail,
                                      Instant occurredAt) {
    }
}
