package com.energymonitor.device.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.energymonitor.device.adapter.out.remote.DeviceChanges;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Device events consumer")
class DeviceEventsConsumerTest {

    private final DeviceChanges changes = mock(DeviceChanges.class);
    private final DeviceEventsConsumer consumer = new DeviceEventsConsumer(changes, JsonMapper.builder().build());

    @Test
    @DisplayName("a link or an unlink forgets the device and its home")
    void linksForget() {
        consumer.on(message("device.linked", "{\"deviceId\":\"DEV0000001\",\"homeId\":\"hom0000001\","
                + "\"deviceName\":\"Nevera\",\"linkedBy\":\"use0000001\",\"linkedAt\":\"2026-10-23T15:00:00Z\"}"));
        consumer.on(message("device.unlinked", "{\"deviceId\":\"DEV0000002\",\"homeId\":\"hom0000002\","
                + "\"unlinkedAt\":\"2026-10-23T15:00:00Z\"}"));

        verify(changes).forget("DEV0000001", "hom0000001");
        verify(changes).forget("DEV0000002", "hom0000002");
    }

    @Test
    @DisplayName("the type is read from the message when the routing key is missing")
    void typeFallback() {
        MessageProperties properties = new MessageProperties();
        properties.setType("device.unlinked");
        consumer.on(new Message("{\"deviceId\":\"DEV0000001\",\"homeId\":\"hom0000001\"}"
                .getBytes(StandardCharsets.UTF_8), properties));

        verify(changes).forget("DEV0000001", "hom0000001");
    }

    @Test
    @DisplayName("a connectivity event, from a queue still bound to it, is dropped: it is the alert service's")
    void connectivityDropped() {
        consumer.on(message("device.connectivity-lost",
                "{\"deviceId\":\"DEV0000001\",\"homeId\":\"hom0000001\",\"occurredAt\":\"2026-10-23T15:00:00Z\"}"));
        consumer.on(message("device.connectivity-restored", "{not json"));

        verifyNoInteractions(changes);
    }

    @Test
    @DisplayName("an unknown type or a message without a device is dropped")
    void dropped() {
        consumer.on(message("device.renamed", "{\"deviceId\":\"DEV0000001\"}"));
        consumer.on(message("device.linked", "{\"homeId\":\"hom0000001\"}"));

        verify(changes, never()).forget(anyString(), anyString());
    }

    @Test
    @DisplayName("an unreadable body fails, so the broker retries it and then dead-letters it")
    void unreadable() {
        assertThatThrownBy(() -> consumer.on(message("device.linked", "{not json")))
                .isInstanceOf(JacksonException.class);
    }

    private static Message message(String routingKey, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        properties.setType(routingKey);
        properties.setMessageId("dmo0000001");
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
