package com.energymonitor.device.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.device.api.DeviceConnectivityLost;
import com.energymonitor.device.api.DeviceConnectivityRestored;
import com.energymonitor.device.api.DeviceLinked;
import com.energymonitor.device.application.port.out.DeviceEventPort;
import com.energymonitor.device.application.port.out.DeviceStatusLogPersistencePort;
import com.energymonitor.device.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.device.application.usecase.DeviceConnectivityService;
import com.energymonitor.device.domain.model.DeviceStatus;
import com.energymonitor.device.domain.model.DeviceStatusLog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceConnectivityServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-07T12:00:00Z");
    private static final String DEVICE = "DEV0000001";

    private MutableClock clock;
    private FakeStatusLogs statusLogs;
    private List<Object> events;
    private DeviceConnectivityService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(T0);
        statusLogs = new FakeStatusLogs();
        events = new ArrayList<>();
        IdentifierGeneratorPort identifiers = new IdentifierGeneratorPort() {
            private int next;

            @Override
            public String nextDeviceId() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String nextDeviceStatusLogId() {
                return String.format("dss%07d", ++next);
            }

            @Override
            public String nextApiKey() {
                throw new UnsupportedOperationException();
            }
        };
        DeviceEventPort eventPort = new DeviceEventPort() {
            @Override
            public void publish(DeviceConnectivityLost event) {
                events.add(event);
            }

            @Override
            public void publish(DeviceConnectivityRestored event) {
                events.add(event);
            }

            @Override
            public void publish(com.energymonitor.device.api.DeviceUnlinked event) {
                events.add(event);
            }

            @Override
            public void publish(DeviceLinked event) {
                events.add(event);
            }
        };
        service = new DeviceConnectivityService(statusLogs, identifiers, eventPort, clock, Duration.ofSeconds(150));
    }

    @Test
    void firstReportOpensAnOnlineLog() {
        service.recordOnline(DEVICE, -60);

        assertEquals(1, statusLogs.rows.size());
        DeviceStatusLog current = statusLogs.findLatestByDeviceId(DEVICE).orElseThrow();
        assertEquals(DeviceStatus.ONLINE, current.status());
        assertEquals(-60, current.signalStrength());
        assertEquals(T0, current.lastSeen());
    }

    @Test
    void repeatedReportsRefreshTheSameRowInsteadOfAddingOne() {
        service.recordOnline(DEVICE, -60);
        clock.advance(Duration.ofSeconds(60));
        service.recordOnline(DEVICE, -55);

        assertEquals(1, statusLogs.rows.size());
        DeviceStatusLog current = statusLogs.findLatestByDeviceId(DEVICE).orElseThrow();
        assertEquals(T0.plusSeconds(60), current.lastSeen());
        assertEquals(-55, current.signalStrength());
    }

    @Test
    void lastWillClosesTheOnlineLogAndPublishesTheEvent() {
        service.recordOnline(DEVICE, -60);
        clock.advance(Duration.ofSeconds(10));
        service.recordOffline(DEVICE);

        assertEquals(2, statusLogs.rows.size());
        assertEquals(DeviceStatus.OFFLINE, statusLogs.findLatestByDeviceId(DEVICE).orElseThrow().status());
        assertEquals(List.of(new DeviceConnectivityLost(DEVICE, T0.plusSeconds(10))), events);
    }

    @Test
    void offlineOnADeviceAlreadyOfflineChangesNothing() {
        service.recordOffline(DEVICE);
        service.recordOnline(DEVICE, -60);
        service.recordOffline(DEVICE);
        service.recordOffline(DEVICE);

        assertEquals(2, statusLogs.rows.size());
        assertEquals(1, events.size());
    }

    @Test
    void comingBackAfterOfflineOpensANewOnlineLog() {
        service.recordOnline(DEVICE, -60);
        clock.advance(Duration.ofSeconds(10));
        service.recordOffline(DEVICE);
        clock.advance(Duration.ofSeconds(10));
        service.recordOnline(DEVICE, -61);

        assertEquals(3, statusLogs.rows.size());
        assertEquals(DeviceStatus.ONLINE, statusLogs.findLatestByDeviceId(DEVICE).orElseThrow().status());
        assertEquals(new DeviceConnectivityRestored(DEVICE, T0.plusSeconds(20)), events.getLast());
    }

    @Test
    void silentDeviceIsMarkedOfflineOnlyAfterTheThreshold() {
        service.recordOnline(DEVICE, -60);

        clock.advance(Duration.ofSeconds(149));
        assertEquals(0, service.markInactiveDevicesOffline());

        clock.advance(Duration.ofSeconds(2));
        assertEquals(1, service.markInactiveDevicesOffline());
        assertEquals(DeviceStatus.OFFLINE, statusLogs.findLatestByDeviceId(DEVICE).orElseThrow().status());
        assertEquals(1, events.size());

        clock.advance(Duration.ofSeconds(300));
        assertEquals(0, service.markInactiveDevicesOffline());
        assertTrue(events.size() == 1);
    }

    /** In-memory port; the latest row of a device is the one with the greatest last-seen time. */
    private static final class FakeStatusLogs implements DeviceStatusLogPersistencePort {

        private final Map<String, DeviceStatusLog> rows = new LinkedHashMap<>();

        @Override
        public void save(DeviceStatusLog log) {
            rows.put(log.idDeviceStatus(), log);
        }

        @Override
        public Optional<DeviceStatusLog> findLatestByDeviceId(String deviceId) {
            return rows.values().stream()
                    .filter(log -> deviceId.equals(log.deviceId()))
                    .max(Comparator.comparing(DeviceStatusLog::lastSeen));
        }

        @Override
        public void touch(String idDeviceStatus, Integer signalStrength, Instant lastSeen) {
            DeviceStatusLog old = rows.get(idDeviceStatus);
            rows.put(idDeviceStatus, new DeviceStatusLog(idDeviceStatus, old.deviceId(), old.status(),
                    signalStrength, lastSeen));
        }

        @Override
        public List<DeviceStatusLog> findOnlineNotSeenSince(Instant cutoff) {
            return rows.values().stream()
                    .map(DeviceStatusLog::deviceId)
                    .distinct()
                    .map(this::findLatestByDeviceId)
                    .flatMap(Optional::stream)
                    .filter(log -> log.status() == DeviceStatus.ONLINE && log.lastSeen().isBefore(cutoff))
                    .toList();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
