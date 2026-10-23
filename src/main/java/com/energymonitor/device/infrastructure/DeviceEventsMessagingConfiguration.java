package com.energymonitor.device.infrastructure;

import com.energymonitor.device.adapter.in.messaging.DeviceEventsConsumer;
import com.energymonitor.device.adapter.out.remote.DeviceChanges;
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
 * The queue through which this backend hears the device service: connectivity changes for the
 * alerts, links and unlinks for the cache of devices.
 *
 * <p>Durable, bound to {@code energy-monitor.device} with the four device routing keys, and
 * dead-lettered to {@value #DEVICE_EVENTS_QUEUE}{@code .dlq} once a message has failed every retry.
 * Declaring it is also what lets the device service publish them: until a queue is bound, its
 * messages wait in its outbox.
 */
@Configuration(proxyBeanMethods = false)
public class DeviceEventsMessagingConfiguration {

    public static final String DEVICE_EVENTS_QUEUE = "energy-monitor-backend.device.events";

    @Bean
    public Declarables deviceEventQueues(
            @Value("${messaging.device-exchange}") String deviceExchange,
            @Value("${messaging.dead-letter-exchange}") String deadLetterExchange) {
        TopicExchange device = new TopicExchange(deviceExchange, true, false);
        DirectExchange deadLetters = new DirectExchange(deadLetterExchange, true, false);
        Queue events = QueueBuilder.durable(DEVICE_EVENTS_QUEUE)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(DEVICE_EVENTS_QUEUE)
                .build();
        Queue dead = QueueBuilder.durable(DEVICE_EVENTS_QUEUE + ".dlq").build();
        return new Declarables(device, deadLetters, events, dead,
                BindingBuilder.bind(events).to(device).with("device.connectivity-lost"),
                BindingBuilder.bind(events).to(device).with("device.connectivity-restored"),
                BindingBuilder.bind(events).to(device).with("device.linked"),
                BindingBuilder.bind(events).to(device).with("device.unlinked"),
                BindingBuilder.bind(dead).to(deadLetters).with(DEVICE_EVENTS_QUEUE));
    }

    /** Forgetting goes to the cache of devices, {@code RemoteDeviceApi}. */
    @Bean
    public DeviceEventsConsumer deviceEventsConsumer(ApplicationEventPublisher events, DeviceChanges changes,
                                                     PlatformTransactionManager transactions,
                                                     ObjectMapper json) {
        return new DeviceEventsConsumer(events, changes, transactions, json);
    }
}
