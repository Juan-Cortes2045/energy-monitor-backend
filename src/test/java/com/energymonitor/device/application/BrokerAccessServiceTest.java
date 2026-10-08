package com.energymonitor.device.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess.Access;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.application.usecase.BrokerAccessService;
import com.energymonitor.device.domain.model.Device;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BrokerAccessServiceTest {

    private static final Device DEVICE = new Device("DEV0000001", "Medidor", "appl000001", null, null,
            Instant.parse("2026-10-01T00:00:00Z"), "EM204", "device-api-key-123456");

    private final DevicePersistencePort devices = new DevicePersistencePort() {
        @Override
        public void save(Device device) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Device> findById(String deviceId) {
            return DEVICE.idDevice().equals(deviceId) ? Optional.of(DEVICE) : Optional.empty();
        }

        @Override
        public Optional<Device> findByDeviceCode(String deviceCode) {
            return DEVICE.deviceCode().equals(deviceCode) ? Optional.of(DEVICE) : Optional.empty();
        }
    };

    private final BrokerAccessService service = new BrokerAccessService(devices, "em-backend", "backend-secret");

    @Test
    void deviceLogsInWithItsCodeAndApiKey() {
        assertTrue(service.authenticate("EM204", "device-api-key-123456"));
        assertFalse(service.authenticate("EM204", "wrong"));
        assertFalse(service.authenticate("EM999", "device-api-key-123456"));
        assertFalse(service.authenticate("EM204", ""));
        assertFalse(service.authenticate(null, null));
    }

    @Test
    void backendLogsInWithItsConfiguredCredentials() {
        assertTrue(service.authenticate("em-backend", "backend-secret"));
        assertFalse(service.authenticate("em-backend", "device-api-key-123456"));
    }

    @Test
    void backendWithoutConfiguredPasswordIsRefused() {
        BrokerAccessService unconfigured = new BrokerAccessService(devices, "em-backend", "");
        assertFalse(unconfigured.authenticate("em-backend", "anything"));
    }

    @Test
    void devicePublishesOnlyOnItsOwnTopics() {
        assertTrue(service.authorize("EM204", "energy-monitor/devices/DEV0000001/telemetry", Access.WRITE));
        assertTrue(service.authorize("EM204", "energy-monitor/devices/DEV0000001/status", Access.WRITE));

        assertFalse(service.authorize("EM204", "energy-monitor/devices/DEV0000002/telemetry", Access.WRITE));
        assertFalse(service.authorize("EM204", "energy-monitor/devices/DEV0000001/other", Access.WRITE));
        assertFalse(service.authorize("EM204", "energy-monitor/devices/DEV0000001/telemetry", Access.READ));
        assertFalse(service.authorize("EM204", "energy-monitor/devices/+/telemetry", Access.SUBSCRIBE));
        assertFalse(service.authorize("EM204", "#", Access.SUBSCRIBE));
    }

    @Test
    void backendReadsEveryDeviceButNeverPublishes() {
        assertTrue(service.authorize("em-backend", "energy-monitor/devices/+/telemetry", Access.SUBSCRIBE));
        assertTrue(service.authorize("em-backend", "energy-monitor/devices/+/status", Access.SUBSCRIBE));
        assertTrue(service.authorize("em-backend", "energy-monitor/devices/DEV0000001/telemetry", Access.READ));

        assertFalse(service.authorize("em-backend", "energy-monitor/devices/DEV0000001/telemetry", Access.WRITE));
        assertFalse(service.authorize("em-backend", "#", Access.SUBSCRIBE));
        assertFalse(service.authorize("em-backend", "energy-monitor/devices/#", Access.SUBSCRIBE));
        assertFalse(service.authorize("em-backend", "$SYS/broker/uptime", Access.READ));
    }

    @Test
    void unknownClientGetsNothing() {
        assertFalse(service.authorize("intruder", "energy-monitor/devices/DEV0000001/telemetry", Access.WRITE));
    }
}
