package com.energymonitor.measurement.adapter.in.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.energymonitor.device.api.ConnectivityStatus;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.measurement.application.command.RegisterMeasurementCommand;
import com.energymonitor.measurement.application.port.in.RegisterMeasurement;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Telemetry ingestion from the broker: the topic, not the payload, identifies the device; a
 * message that cannot be trusted or parsed is dropped without stopping the subscription.
 *
 * <p>Plain Mockito test: the consumer is built by hand with a real {@code JsonMapper} and mocked
 * ports, so no Spring context or database is needed.
 */
@ExtendWith(MockitoExtension.class)
class TelemetryMqttConsumerTest {

    private static final String DEVICE_ID = "DEV0000001";
    private static final String TOPIC = "energy-monitor/devices/" + DEVICE_ID + "/telemetry";

    @Mock
    private DeviceApi deviceApi;

    @Mock
    private RegisterMeasurement registerMeasurement;

    private TelemetryMqttConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TelemetryMqttConsumer(JsonMapper.builder().build(), deviceApi, registerMeasurement);
    }

    private static Message<String> message(String topic, String payload) {
        return MessageBuilder.withPayload(payload).setHeader(MqttHeaders.RECEIVED_TOPIC, topic).build();
    }

    private static String payload(String deviceId) {
        String id = deviceId == null ? "" : "\"deviceId\":\"" + deviceId + "\",";
        return "{" + id + "\"dateTime\":\"2026-10-13T20:17:03Z\",\"voltage\":231.4,\"current\":1.234,"
                + "\"activePower\":285.5,\"storedEnergy\":12.847,\"rssi\":-58}";
    }

    @Test
    @DisplayName("a valid reading is registered with the device timestamp and marks the device online")
    void registersValidReading() {
        // Arrange
        when(deviceApi.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(mock(DeviceDto.class)));

        // Act
        consumer.handleTelemetry(message(TOPIC, payload(DEVICE_ID)));

        // Assert
        ArgumentCaptor<RegisterMeasurementCommand> captor = ArgumentCaptor.forClass(RegisterMeasurementCommand.class);
        verify(registerMeasurement, times(1)).register(captor.capture());
        RegisterMeasurementCommand command = captor.getValue();
        assertThat(command.deviceId()).isEqualTo(DEVICE_ID);
        assertThat(command.dateTime()).isEqualTo(Instant.parse("2026-10-13T20:17:03Z"));
        assertThat(command.activePower()).isEqualTo(285.5);
        assertThat(command.storedEnergy()).isEqualTo(12.847);
        verify(deviceApi, times(1)).recordConnectivity(DEVICE_ID, ConnectivityStatus.ONLINE, -58);
    }

    @Test
    @DisplayName("a payload without deviceId is accepted because the topic identifies the device")
    void acceptsPayloadWithoutDeviceId() {
        // Arrange
        when(deviceApi.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(mock(DeviceDto.class)));

        // Act
        consumer.handleTelemetry(message(TOPIC, payload(null)));

        // Assert
        verify(registerMeasurement, times(1)).register(any());
    }

    @Test
    @DisplayName("a payload that claims another device is dropped")
    void dropsPayloadOfAnotherDevice() {
        // Act
        consumer.handleTelemetry(message(TOPIC, payload("DEV0000002")));

        // Assert
        verify(registerMeasurement, never()).register(any());
        verify(deviceApi, never()).recordConnectivity(any(), any(), anyInt());
    }

    @Test
    @DisplayName("a reading of an unknown device is dropped")
    void dropsUnknownDevice() {
        // Arrange
        when(deviceApi.findByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());

        // Act
        consumer.handleTelemetry(message(TOPIC, payload(DEVICE_ID)));

        // Assert
        verify(registerMeasurement, never()).register(any());
    }

    @ParameterizedTest(name = "topic \"{0}\" is ignored")
    @ValueSource(strings = {"energy-monitor/devices/telemetry", "energy-monitor/devices//telemetry",
            "energy-monitor/devices/DEV0000001/telemetry/extra"})
    @DisplayName("a message on an unexpected topic is ignored")
    void ignoresUnexpectedTopic(String topic) {
        // Act
        consumer.handleTelemetry(message(topic, payload(DEVICE_ID)));

        // Assert
        verifyNoInteractions(deviceApi, registerMeasurement);
    }

    @Test
    @DisplayName("a message without a topic header is ignored")
    void ignoresMessageWithoutTopic() {
        // Act
        consumer.handleTelemetry(MessageBuilder.withPayload(payload(DEVICE_ID)).build());

        // Assert
        verifyNoInteractions(deviceApi, registerMeasurement);
    }

    @Test
    @DisplayName("a malformed payload is dropped without throwing")
    void survivesMalformedPayload() {
        // Act + Assert
        assertThatCode(() -> consumer.handleTelemetry(message(TOPIC, "{not json"))).doesNotThrowAnyException();
        verify(registerMeasurement, never()).register(any());
    }

    @Test
    @DisplayName("a failure while registering does not escape and the device is not marked online")
    void survivesRegistrationFailure() {
        // Arrange
        when(deviceApi.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(mock(DeviceDto.class)));
        when(registerMeasurement.register(any())).thenThrow(new IllegalStateException("database unavailable"));

        // Act + Assert
        assertThatCode(() -> consumer.handleTelemetry(message(TOPIC, payload(DEVICE_ID)))).doesNotThrowAnyException();
        verify(deviceApi, never()).recordConnectivity(eq(DEVICE_ID), any(), any());
    }
}
