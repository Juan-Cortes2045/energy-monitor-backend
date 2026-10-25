package com.energymonitor.measurement.infrastructure;

import com.energymonitor.measurement.adapter.in.messaging.MeasurementRecordedConsumer;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

/**
 * The queue through which this backend hears every reading the measurement service stores.
 *
 * <p>Durable, bound to {@code energy-monitor.measurement} with {@code measurement.recorded}, and
 * dead-lettered to {@value #MEASUREMENT_RECORDED_QUEUE}{@code .dlq} once a message has failed every
 * retry. Declaring it is also what lets the measurement service publish them: until a queue is
 * bound, its messages wait in its outbox.
 */
@Configuration(proxyBeanMethods = false)
public class MeasurementEventsMessagingConfiguration {

    public static final String MEASUREMENT_RECORDED_QUEUE = "energy-monitor-backend.measurement.recorded";

    @Bean
    public Declarables measurementEventQueues(
            @Value("${messaging.measurement-exchange}") String measurementExchange,
            @Value("${messaging.dead-letter-exchange}") String deadLetterExchange) {
        TopicExchange measurement = new TopicExchange(measurementExchange, true, false);
        DirectExchange deadLetters = new DirectExchange(deadLetterExchange, true, false);
        Queue recorded = QueueBuilder.durable(MEASUREMENT_RECORDED_QUEUE)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(MEASUREMENT_RECORDED_QUEUE)
                .build();
        Queue dead = QueueBuilder.durable(MEASUREMENT_RECORDED_QUEUE + ".dlq").build();
        return new Declarables(measurement, deadLetters, recorded, dead,
                BindingBuilder.bind(recorded).to(measurement).with("measurement.recorded"),
                BindingBuilder.bind(dead).to(deadLetters).with(MEASUREMENT_RECORDED_QUEUE));
    }

    @Bean
    public MeasurementRecordedConsumer measurementRecordedConsumer(ApplicationEventPublisher events,
                                                                   PlatformTransactionManager transactions,
                                                                   ObjectMapper json) {
        return new MeasurementRecordedConsumer(events, transactions, json);
    }
}
