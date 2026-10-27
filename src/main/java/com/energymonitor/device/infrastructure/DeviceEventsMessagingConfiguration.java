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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * The queue through which this backend hears the links and unlinks of the device service, for
 * the cache of devices.
 *
 * <p>Durable, bound to {@code energy-monitor.device} with {@code device.linked} and
 * {@code device.unlinked}, and dead-lettered to {@value #DEVICE_EVENTS_QUEUE}{@code .dlq} once a
 * message has failed every retry. The connectivity events are the alert service's; a queue an
 * earlier version bound to them keeps those bindings until it is deleted and declared again.
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
                BindingBuilder.bind(events).to(device).with("device.linked"),
                BindingBuilder.bind(events).to(device).with("device.unlinked"),
                BindingBuilder.bind(dead).to(deadLetters).with(DEVICE_EVENTS_QUEUE));
    }

    /** Forgetting goes to the cache of devices, {@code RemoteDeviceApi}. */
    @Bean
    public DeviceEventsConsumer deviceEventsConsumer(DeviceChanges changes, ObjectMapper json) {
        return new DeviceEventsConsumer(changes, json);
    }
}
