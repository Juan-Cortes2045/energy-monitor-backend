package com.energymonitor.notification.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The queues through which this module hears from the security service.
 *
 * <ul>
 *   <li>{@value #ACCOUNT_EVENTS_QUEUE}: new accounts and administrators' changes, which become
 *       in-app notifications for the administrators.</li>
 *   <li>{@value #MAIL_QUEUE}: verification and recovery codes to mail.</li>
 * </ul>
 *
 * <p>Both are durable and dead-letter to a queue of their own once a message has failed every
 * retry, so a message that cannot be handled is kept for inspection instead of blocking the
 * ones behind it.
 */
@Configuration(proxyBeanMethods = false)
public class NotificationMessagingConfiguration {

    public static final String ACCOUNT_EVENTS_QUEUE = "energy-monitor-backend.notification.account-events";
    public static final String MAIL_QUEUE = "energy-monitor-backend.notification.mail";

    @Bean
    public Declarables notificationQueues(
            @Value("${messaging.security-exchange}") String securityExchange,
            @Value("${messaging.dead-letter-exchange}") String deadLetterExchange) {
        TopicExchange security = new TopicExchange(securityExchange, true, false);
        DirectExchange deadLetters = new DirectExchange(deadLetterExchange, true, false);

        Queue accountEvents = queue(ACCOUNT_EVENTS_QUEUE, deadLetterExchange);
        Queue mail = queue(MAIL_QUEUE, deadLetterExchange);
        Queue accountEventsDead = QueueBuilder.durable(ACCOUNT_EVENTS_QUEUE + ".dlq").build();
        Queue mailDead = QueueBuilder.durable(MAIL_QUEUE + ".dlq").build();

        return new Declarables(security, deadLetters, accountEvents, mail, accountEventsDead, mailDead,
                bind(accountEvents, security, "account.registered"),
                bind(accountEvents, security, "account.administered"),
                bind(mail, security, "mail.email-verification"),
                bind(mail, security, "mail.password-reset"),
                BindingBuilder.bind(accountEventsDead).to(deadLetters).with(ACCOUNT_EVENTS_QUEUE),
                BindingBuilder.bind(mailDead).to(deadLetters).with(MAIL_QUEUE));
    }

    private static Queue queue(String name, String deadLetterExchange) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(name)
                .build();
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
