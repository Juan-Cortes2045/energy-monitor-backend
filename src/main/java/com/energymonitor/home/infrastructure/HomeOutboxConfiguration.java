package com.energymonitor.home.infrastructure;

import com.energymonitor.home.adapter.out.messaging.HomeOutboxPublisher;
import com.energymonitor.home.adapter.out.messaging.HomeOutboxRelay;
import com.energymonitor.home.adapter.out.messaging.RabbitHomeOutboxPublisher;
import com.energymonitor.home.adapter.out.persistence.repository.HomeOutboxRepository;
import java.time.Clock;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * The broker side of the home outbox: the exchange the home messages go to, and the relay.
 *
 * <p>Only the exchange is declared here. The queues belong to the consumers, which declare and
 * bind their own; until one exists for a message type, {@link RabbitHomeOutboxPublisher} gets
 * the message back and the relay keeps it pending.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HomeOutboxProperties.class)
public class HomeOutboxConfiguration {

    @Bean
    public TopicExchange homeExchange(HomeOutboxProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    /**
     * A template of its own: being mandatory, with a returns callback, is the outbox's business
     * and must not change how anything else in the backend sends.
     */
    @Bean
    public HomeOutboxPublisher homeOutboxPublisher(ConnectionFactory connections,
                                                   HomeOutboxProperties properties) {
        return new RabbitHomeOutboxPublisher(new RabbitTemplate(connections), properties.exchange(),
                properties.confirmTimeout());
    }

    @Bean
    public HomeOutboxRelay homeOutboxRelay(HomeOutboxRepository outbox, HomeOutboxPublisher publisher,
                                           HomeOutboxProperties properties, Clock clock) {
        return new HomeOutboxRelay(outbox, publisher, clock, properties.batchSize());
    }

    /** Runs the relay on a fixed delay, so a slow run never overlaps the next one. */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "messaging.home.relay-enabled", havingValue = "true", matchIfMissing = true)
    static class RelaySchedule implements SchedulingConfigurer {

        private final HomeOutboxRelay relay;
        private final HomeOutboxProperties properties;

        RelaySchedule(HomeOutboxRelay relay, HomeOutboxProperties properties) {
            this.relay = relay;
            this.properties = properties;
        }

        @Override
        public void configureTasks(ScheduledTaskRegistrar registrar) {
            registrar.addFixedDelayTask(relay::relay, properties.pollInterval());
        }
    }
}
