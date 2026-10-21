package com.energymonitor.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.energymonitor.home.infrastructure.HomeMessagingConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

/**
 * The queues the backend consumes from the security service: what each one is bound to, and
 * where a message goes after failing every retry. A routing key that does not match the one the
 * security service publishes silently stops a kind of message, so the keys are pinned here.
 */
@DisplayName("Messaging topology")
class MessagingTopologyTest {

    private static final String EXCHANGE = "energy-monitor.security";
    private static final String DLX = "energy-monitor-backend.dlx";

    @Test
    @DisplayName("notification hears account events and mail requests")
    void notificationQueues() {
        Declarables declarables = new NotificationMessagingConfiguration()
                .notificationQueues(EXCHANGE, DLX);

        assertThat(bindingsFrom(declarables, EXCHANGE))
                .extracting(Binding::getDestination, Binding::getRoutingKey)
                .containsExactlyInAnyOrder(
                        tuple(NotificationMessagingConfiguration.ACCOUNT_EVENTS_QUEUE, "account.registered"),
                        tuple(NotificationMessagingConfiguration.ACCOUNT_EVENTS_QUEUE, "account.administered"),
                        tuple(NotificationMessagingConfiguration.MAIL_QUEUE, "mail.email-verification"),
                        tuple(NotificationMessagingConfiguration.MAIL_QUEUE, "mail.password-reset"));
        assertDeadLettered(declarables, NotificationMessagingConfiguration.ACCOUNT_EVENTS_QUEUE);
        assertDeadLettered(declarables, NotificationMessagingConfiguration.MAIL_QUEUE);
    }

    @Test
    @DisplayName("home hears account deletions")
    void homeQueue() {
        Declarables declarables = new HomeMessagingConfiguration().homeQueues(EXCHANGE, DLX);

        assertThat(bindingsFrom(declarables, EXCHANGE))
                .extracting(Binding::getDestination, Binding::getRoutingKey)
                .containsExactly(tuple(HomeMessagingConfiguration.ACCOUNT_DELETED_QUEUE, "account.deleted"));
        assertDeadLettered(declarables, HomeMessagingConfiguration.ACCOUNT_DELETED_QUEUE);
    }

    private static void assertDeadLettered(Declarables declarables, String queueName) {
        Queue queue = declarables.getDeclarablesByType(Queue.class).stream()
                .filter(candidate -> candidate.getName().equals(queueName))
                .findFirst().orElseThrow();
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", DLX)
                .containsEntry("x-dead-letter-routing-key", queueName);
        assertThat(bindingsFrom(declarables, DLX))
                .extracting(Binding::getDestination, Binding::getRoutingKey)
                .contains(tuple(queueName + ".dlq", queueName));
    }

    private static List<Binding> bindingsFrom(Declarables declarables, String exchange) {
        return declarables.getDeclarablesByType(Binding.class).stream()
                .filter(binding -> binding.getExchange().equals(exchange))
                .toList();
    }
}
