package com.energymonitor.device.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.energymonitor.alert.api.AlertStatus;
import com.energymonitor.alert.api.AlertType;
import com.energymonitor.alert.application.port.out.AlertPersistencePort;
import com.energymonitor.alert.domain.model.Alert;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.Device;
import com.energymonitor.device.domain.model.DeviceHome;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The connectivity events of the device service reach the real alert module: a lost connection
 * raises one CONNECTIVITY alert, however many times it is delivered, and a restored one resolves
 * it. {@code @Transactional}: everything joins the test's transaction, which is rolled back.
 */
@SpringBootTest(properties = "device.service.events-enabled=true")
@Transactional
@DisplayName("Device events, end to end in the backend")
class DeviceEventsConsumerIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-10-23T15:00:00Z");
    private static final String DEVICE = "dee0000001";
    private static final String HOME = "deh0000001";

    @Autowired
    private DeviceEventsConsumer consumer;
    @Autowired
    private DevicePersistencePort devices;
    @Autowired
    private DeviceHomePersistencePort deviceHomes;
    @Autowired
    private AlertPersistencePort alerts;

    @BeforeEach
    void linkDevice() {
        // Linked in this backend's tables, which still answer DeviceApi until the module goes.
        devices.save(Device.create(DEVICE, "Nevera", "appl000001", null, null, NOW, "DE0001", "de-api-key"));
        deviceHomes.save(new DeviceHome(DEVICE, HOME));
    }

    @Test
    @DisplayName("a lost connection raises one alert, even delivered twice, and a restored one resolves it")
    void raisesAndResolves() {
        consumer.on(message("device.connectivity-lost"));
        consumer.on(message("device.connectivity-lost"));

        assertThat(pendingConnectivity()).hasSize(1);

        consumer.on(message("device.connectivity-restored"));

        assertThat(pendingConnectivity()).isEmpty();
    }

    private List<Alert> pendingConnectivity() {
        return alerts.listActiveByHome(HOME, AlertStatus.PENDING).stream()
                .filter(alert -> alert.type() == AlertType.CONNECTIVITY && DEVICE.equals(alert.deviceId()))
                .toList();
    }

    private static Message message(String type) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(type);
        properties.setType(type);
        properties.setMessageId("dmo0000001");
        String body = "{\"deviceId\":\"" + DEVICE + "\",\"homeId\":\"" + HOME
                + "\",\"occurredAt\":\"2026-10-23T15:00:00Z\"}";
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
