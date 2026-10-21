package com.energymonitor.home.infrastructure;

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
 * The queue through which this module hears that the security service deleted an account, so
 * the account's memberships, and the homes only it owned, go too.
 *
 * <p>Durable, and dead-lettered to {@value #ACCOUNT_DELETED_QUEUE}{@code .dlq} once a message
 * has failed every retry.
 */
@Configuration(proxyBeanMethods = false)
public class HomeMessagingConfiguration {

    public static final String ACCOUNT_DELETED_QUEUE = "energy-monitor-backend.home.account-deleted";

    @Bean
    public Declarables homeQueues(
            @Value("${messaging.security-exchange}") String securityExchange,
            @Value("${messaging.dead-letter-exchange}") String deadLetterExchange) {
        TopicExchange security = new TopicExchange(securityExchange, true, false);
        DirectExchange deadLetters = new DirectExchange(deadLetterExchange, true, false);
        Queue accountDeleted = QueueBuilder.durable(ACCOUNT_DELETED_QUEUE)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(ACCOUNT_DELETED_QUEUE)
                .build();
        Queue accountDeletedDead = QueueBuilder.durable(ACCOUNT_DELETED_QUEUE + ".dlq").build();
        return new Declarables(security, deadLetters, accountDeleted, accountDeletedDead,
                BindingBuilder.bind(accountDeleted).to(security).with("account.deleted"),
                BindingBuilder.bind(accountDeletedDead).to(deadLetters).with(ACCOUNT_DELETED_QUEUE));
    }
}
