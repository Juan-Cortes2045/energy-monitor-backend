package com.energymonitor.measurement.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.energymonitor.measurement.api.MeasurementRecorded;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Measurement recorded consumer")
class MeasurementRecordedConsumerTest {

    private static final String READING = "{\"idMeasurement\":\"MEA0000001\",\"deviceId\":\"DEV0000001\","
            + "\"dateTime\":\"2026-10-24T20:00:00Z\",\"voltage\":231.4,\"current\":1.2,"
            + "\"activePower\":285.5,\"storedEnergy\":12.8}";

    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final MeasurementRecordedConsumer consumer = new MeasurementRecordedConsumer(events,
            mock(PlatformTransactionManager.class), JsonMapper.builder().build());

    @Test
    @DisplayName("a reading is published in memory for the alerts")
    void reading() {
        consumer.on(message("measurement.recorded", READING));

        verify(events).publishEvent(new MeasurementRecorded("MEA0000001", "DEV0000001",
                Instant.parse("2026-10-24T20:00:00Z"), 231.4, 1.2, 285.5, 12.8));
    }

    @Test
    @DisplayName("the type is read from the message when the routing key is missing")
    void typeFallback() {
        MessageProperties properties = new MessageProperties();
        properties.setType("measurement.recorded");
        consumer.on(new Message(READING.getBytes(StandardCharsets.UTF_8), properties));

        verify(events).publishEvent(any(MeasurementRecorded.class));
    }

    @Test
    @DisplayName("another type, or a reading without a device or a time, is dropped")
    void dropped() {
        consumer.on(message("measurement.deleted", READING));
        consumer.on(message("measurement.recorded", READING.replace("\"deviceId\":\"DEV0000001\",", "")));
        consumer.on(message("measurement.recorded", READING.replace("\"dateTime\":\"2026-10-24T20:00:00Z\",", "")));

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("an unreadable body fails, so the broker retries it and then dead-letters it")
    void unreadable() {
        assertThatThrownBy(() -> consumer.on(message("measurement.recorded", "{not json")))
                .isInstanceOf(JacksonException.class);
    }

    private static Message message(String routingKey, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        properties.setType(routingKey);
        properties.setMessageId("MMO0000001");
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
