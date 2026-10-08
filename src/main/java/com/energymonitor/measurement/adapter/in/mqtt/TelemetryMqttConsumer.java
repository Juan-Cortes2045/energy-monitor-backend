package com.energymonitor.measurement.adapter.in.mqtt;

import com.energymonitor.device.api.ConnectivityStatus;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.measurement.adapter.in.mqtt.dto.TelemetryMessage;
import com.energymonitor.measurement.application.command.RegisterMeasurementCommand;
import com.energymonitor.measurement.application.port.in.RegisterMeasurement;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns every message on {@code energy-monitor/devices/+/telemetry} into a measurement.
 *
 * <p>The measurement keeps the device timestamp, so samples replayed from the offline queue land
 * at the time they were taken. Connectivity is refreshed with the server clock instead.
 */
@Component
public class TelemetryMqttConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryMqttConsumer.class);

    private final ObjectMapper objectMapper;
    private final DeviceApi deviceApi;
    private final RegisterMeasurement registerMeasurement;

    public TelemetryMqttConsumer(ObjectMapper objectMapper, DeviceApi deviceApi, RegisterMeasurement registerMeasurement) {
        this.objectMapper = objectMapper;
        this.deviceApi = deviceApi;
        this.registerMeasurement = registerMeasurement;
    }

    @ServiceActivator(inputChannel = "mqttTelemetryChannel")
    public void handleTelemetry(Message<?> message) {
        Optional<String> topicDevice = MqttTopics.deviceId(message);
        if (topicDevice.isEmpty()) {
            log.warn("Ignoring telemetry on unexpected topic");
            return;
        }
        String deviceId = topicDevice.get();
        try {
            TelemetryMessage telemetry = objectMapper.readValue(message.getPayload().toString(), TelemetryMessage.class);
            if (telemetry.deviceId() != null && !telemetry.deviceId().equals(deviceId)) {
                log.warn("Ignoring telemetry on the topic of {} that claims to be {}", deviceId, telemetry.deviceId());
                return;
            }
            if (deviceApi.findByDeviceId(deviceId).isEmpty()) {
                log.warn("Ignoring telemetry of unknown device {}", deviceId);
                return;
            }
            registerMeasurement.register(new RegisterMeasurementCommand(
                    deviceId,
                    telemetry.dateTime(),
                    telemetry.voltage(),
                    telemetry.current(),
                    telemetry.activePower(),
                    telemetry.storedEnergy()));
            deviceApi.recordConnectivity(deviceId, ConnectivityStatus.ONLINE, telemetry.rssi());
        } catch (JacksonException e) {
            log.warn("Failed to parse telemetry of device {}: {}", deviceId, e.getOriginalMessage());
        } catch (RuntimeException e) {
            log.warn("Failed to process telemetry of device {}: {}", deviceId, e.getMessage());
        }
    }
}
