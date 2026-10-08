package com.energymonitor.measurement.adapter.in.mqtt;

import java.util.Optional;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;

/**
 * Reads the device id out of {@code energy-monitor/devices/{deviceId}/{kind}}.
 *
 * <p>The broker ACL only lets a device publish on its own topics, so the topic, not the
 * {@code deviceId} field of the payload, is what proves which device sent a message.
 */
final class MqttTopics {

    private MqttTopics() {
    }

    static Optional<String> deviceId(Message<?> message) {
        Object topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        if (topic == null) {
            return Optional.empty();
        }
        String[] parts = topic.toString().split("/");
        return parts.length == 4 && !parts[2].isBlank() ? Optional.of(parts[2]) : Optional.empty();
    }
}
