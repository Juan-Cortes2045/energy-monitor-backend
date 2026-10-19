package com.energymonitor.measurement.adapter.in.mqtt;

import com.energymonitor.device.api.ConnectivityStatus;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.measurement.adapter.in.mqtt.dto.StatusMessage;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Applies the retained {@code online} / {@code offline} messages of
 * {@code energy-monitor/devices/+/status}, including the last will the broker publishes when a
 * device drops without saying goodbye.
 */
@Component
public class StatusMqttConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatusMqttConsumer.class);

    private final ObjectMapper objectMapper;
    private final DeviceApi deviceApi;

    public StatusMqttConsumer(ObjectMapper objectMapper, DeviceApi deviceApi) {
        this.objectMapper = objectMapper;
        this.deviceApi = deviceApi;
    }

    @ServiceActivator(inputChannel = "mqttStatusChannel")
    public void handleStatus(Message<?> message) {
        Optional<String> topicDevice = MqttTopics.deviceId(message);
        if (topicDevice.isEmpty()) {
            log.warn("Ignoring status on unexpected topic");
            return;
        }
        String deviceId = topicDevice.get();
        String payload = message.getPayload().toString();
        if (payload.isBlank()) {
            // An empty retained message only clears the topic.
            return;
        }
        try {
            StatusMessage status = objectMapper.readValue(payload, StatusMessage.class);
            if (status.deviceId() != null && !status.deviceId().equals(deviceId)) {
                log.warn("Ignoring status on the topic of {} that claims to be {}", deviceId, status.deviceId());
                return;
            }
            ConnectivityStatus reported;
            if ("online".equalsIgnoreCase(status.status())) {
                reported = ConnectivityStatus.ONLINE;
            } else if ("offline".equalsIgnoreCase(status.status())) {
                reported = ConnectivityStatus.OFFLINE;
            } else {
                log.warn("Ignoring status '{}' of device {}", status.status(), deviceId);
                return;
            }
            if (deviceApi.findByDeviceId(deviceId).isEmpty()) {
                log.warn("Ignoring status of unknown device {}", deviceId);
                return;
            }
            deviceApi.recordConnectivity(deviceId, reported, status.rssi());
        } catch (JacksonException e) {
            log.warn("Failed to parse status of device {}: {}", deviceId, e.getOriginalMessage());
        } catch (RuntimeException e) {
            log.warn("Failed to process status of device {}: {}", deviceId, e.getMessage());
        }
    }
}
