package com.energymonitor.measurement.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.energymonitor.alert.api.AlertStatus;
import com.energymonitor.alert.api.AlertType;
import com.energymonitor.alert.application.port.out.AlertPersistencePort;
import com.energymonitor.alert.domain.model.Alert;
import com.energymonitor.device.adapter.out.remote.DeviceServiceClient;
import com.energymonitor.device.adapter.out.remote.DeviceServiceClient.RemoteDevice;
import com.energymonitor.measurement.adapter.out.remote.MeasurementServiceClient;
import com.energymonitor.measurement.adapter.out.remote.MeasurementServiceClient.RemoteLevel;
import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * The readings of the measurement service reach the real alert module: a HIGH reading raises one
 * THRESHOLD alert, however many times it is delivered, and a LOW one resolves it. The device and
 * the levels are answered by mocked clients of the two services.
 * {@code @Transactional}: everything joins the test's transaction, which is rolled back.
 */
@SpringBootTest
@Transactional
@DisplayName("Recorded readings, end to end in the backend")
class MeasurementRecordedConsumerIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-10-24T20:00:00Z");
    private static final String DEVICE = "mre0000001";
    private static final String HOME = "mrh0000001";

    @Autowired
    private MeasurementRecordedConsumer consumer;
    @MockitoBean
    private DeviceServiceClient deviceService;
    @MockitoBean
    private MeasurementServiceClient measurementService;
    @Autowired
    private AlertPersistencePort alerts;

    @BeforeEach
    void linkDeviceAndLevels() {
        given(deviceService.device(DEVICE)).willReturn(Optional.of(
                new RemoteDevice(DEVICE, "Nevera", "appl000001", null, null, NOW, "MR0001", HOME)));
        given(deviceService.devicesOfHome(HOME)).willReturn(List.of(
                new RemoteDevice(DEVICE, "Nevera", "appl000001", null, null, NOW, "MR0001", HOME)));
        given(measurementService.levels()).willReturn(List.of(
                new RemoteLevel("lev0000001", RiskConsumption.LOW, "Bajo", 0.0, 1500.0),
                new RemoteLevel("lev0000003", RiskConsumption.HIGH, "Alto", 1500.0, 3000.0)));
        given(measurementService.hourlyEnergy(any(), any(), any())).willReturn(List.of());
    }

    @Test
    @DisplayName("a high reading raises one alert, even delivered twice, and a low one resolves it")
    void raisesAndResolves() {
        consumer.on(message("MEA0000001", 2000.0));
        consumer.on(message("MEA0000001", 2000.0));

        assertThat(pendingThreshold()).hasSize(1)
                .allMatch(alert -> "MEA0000001".equals(alert.measurementId()));

        consumer.on(message("MEA0000002", 300.0));

        assertThat(pendingThreshold()).isEmpty();
    }

    private List<Alert> pendingThreshold() {
        return alerts.listActiveByHome(HOME, AlertStatus.PENDING).stream()
                .filter(alert -> alert.type() == AlertType.THRESHOLD && DEVICE.equals(alert.deviceId()))
                .toList();
    }

    private static Message message(String idMeasurement, double activePower) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey("measurement.recorded");
        properties.setType("measurement.recorded");
        properties.setMessageId("MMO" + idMeasurement.substring(3));
        String body = "{\"idMeasurement\":\"" + idMeasurement + "\",\"deviceId\":\"" + DEVICE
                + "\",\"dateTime\":\"2026-10-24T20:00:00Z\",\"voltage\":230.0,\"current\":1.0,"
                + "\"activePower\":" + activePower + ",\"storedEnergy\":12.8}";
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
