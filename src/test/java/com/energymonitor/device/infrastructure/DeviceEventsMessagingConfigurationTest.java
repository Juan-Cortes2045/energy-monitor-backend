package com.energymonitor.device.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

@DisplayName("Device events queue")
class DeviceEventsMessagingConfigurationTest {

    private final Declarables declarables = new DeviceEventsMessagingConfiguration()
            .deviceEventQueues("energy-monitor.device", "energy-monitor-backend.dlx");

    @Test
    @DisplayName("the durable queue takes the four device messages from the device exchange")
    void bindsTheFourTypes() {
        assertThat(declarables.getDeclarablesByType(Binding.class).stream()
                .filter(b -> b.getDestination().equals(DeviceEventsMessagingConfiguration.DEVICE_EVENTS_QUEUE)))
                .allMatch(b -> b.getExchange().equals("energy-monitor.device"))
                .extracting(Binding::getRoutingKey)
                .containsExactlyInAnyOrder("device.connectivity-lost", "device.connectivity-restored",
                        "device.linked", "device.unlinked");
    }

    @Test
    @DisplayName("a message that keeps failing goes to its dead-letter queue")
    void deadLetters() {
        Queue queue = declarables.getDeclarablesByType(Queue.class).stream()
                .filter(q -> q.getName().equals(DeviceEventsMessagingConfiguration.DEVICE_EVENTS_QUEUE))
                .findFirst().orElseThrow();

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "energy-monitor-backend.dlx")
                .containsEntry("x-dead-letter-routing-key", DeviceEventsMessagingConfiguration.DEVICE_EVENTS_QUEUE);
    }
}
