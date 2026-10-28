package com.energymonitor.outbox.infrastructure;

import com.energymonitor.outbox.adapter.out.messaging.OutboxPublisher;
import com.energymonitor.outbox.adapter.out.messaging.OutboxRelay;
import com.energymonitor.outbox.adapter.out.messaging.RabbitOutboxPublisher;
import com.energymonitor.outbox.adapter.out.persistence.JpaOutboxWriter;
import com.energymonitor.outbox.adapter.out.persistence.repository.OutboxMessageRepository;
import com.energymonitor.outbox.api.OutboxWriter;
import java.time.Clock;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import tools.jackson.databind.ObjectMapper;

/**
 * The outbox: its writer, the publisher with confirms and the relay.
 *
 * <p>No exchange is declared here: each module that writes events declares its own. The queues
 * belong to the consumers, which declare and bind their own; until one exists for a message type,
 * {@link RabbitOutboxPublisher} gets the message back and the relay keeps it pending.
 *
 * <p>Its own UTC clock, so it does not depend on another module's.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {

    private static final Clock CLOCK = Clock.systemUTC();

    @Bean
    public OutboxWriter outboxWriter(OutboxMessageRepository outbox, ObjectMapper json,
                                     OutboxProperties properties) {
        return new JpaOutboxWriter(outbox, json, CLOCK, properties.discardWritten());
    }

    /**
     * A template of its own: being mandatory, with a returns callback, is the outbox's business
     * and must not change how anything else in the backend sends.
     */
    @Bean
    public OutboxPublisher outboxPublisher(ConnectionFactory connections, OutboxProperties properties) {
        return new RabbitOutboxPublisher(new RabbitTemplate(connections), properties.confirmTimeout());
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxMessageRepository outbox, OutboxPublisher publisher,
                                   OutboxProperties properties) {
        return new OutboxRelay(outbox, publisher, CLOCK, properties.batchSize());
    }

    /** Runs the relay on a fixed delay, so a slow run never overlaps the next one. */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "messaging.outbox.relay-enabled", havingValue = "true")
    static class RelaySchedule implements SchedulingConfigurer {

        private final OutboxRelay relay;
        private final OutboxProperties properties;

        RelaySchedule(OutboxRelay relay, OutboxProperties properties) {
            this.relay = relay;
            this.properties = properties;
        }

        @Override
        public void configureTasks(ScheduledTaskRegistrar registrar) {
            registrar.addFixedDelayTask(relay::relay, properties.pollInterval());
        }
    }
}
