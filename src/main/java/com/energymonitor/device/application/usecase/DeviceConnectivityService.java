package com.energymonitor.device.application.usecase;

import com.energymonitor.device.api.DeviceConnectivityLost;
import com.energymonitor.device.api.DeviceConnectivityRestored;
import com.energymonitor.device.application.port.in.DetectInactiveDevices;
import com.energymonitor.device.application.port.in.RecordDeviceConnectivity;
import com.energymonitor.device.application.port.out.DeviceEventPort;
import com.energymonitor.device.application.port.out.DeviceStatusLogPersistencePort;
import com.energymonitor.device.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.device.domain.model.DeviceStatus;
import com.energymonitor.device.domain.model.DeviceStatusLog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Keeps {@code device_status_log} as a history of transitions.
 *
 * <p>The latest row of a device is its current state. An {@code ONLINE} report on a device that
 * is already online only refreshes {@code last_seen}, so a device publishing every 60 seconds
 * does not add a row per minute. A new row is written when the state changes, and every
 * {@code ONLINE -> OFFLINE} transition publishes {@link DeviceConnectivityLost}, every
 * {@code OFFLINE -> ONLINE} one {@link DeviceConnectivityRestored}.
 *
 * <p>Telemetry travels at QoS 0 and the last will is only sent when the broker notices the
 * connection dropped, so a device can disappear without any message. {@link #markInactiveDevicesOffline()}
 * covers that case: a device not seen for {@code offlineAfter} is considered offline.
 *
 * <p>This service contains no Spring annotations.
 */
public class DeviceConnectivityService implements RecordDeviceConnectivity, DetectInactiveDevices {

    private final DeviceStatusLogPersistencePort statusLogs;
    private final IdentifierGeneratorPort identifiers;
    private final DeviceEventPort events;
    private final Clock clock;
    private final Duration offlineAfter;

    public DeviceConnectivityService(DeviceStatusLogPersistencePort statusLogs,
                                     IdentifierGeneratorPort identifiers,
                                     DeviceEventPort events,
                                     Clock clock,
                                     Duration offlineAfter) {
        this.statusLogs = statusLogs;
        this.identifiers = identifiers;
        this.events = events;
        this.clock = clock;
        this.offlineAfter = offlineAfter;
    }

    @Override
    public void recordOnline(String deviceId, Integer signalStrength) {
        Optional<DeviceStatusLog> current = statusLogs.findLatestByDeviceId(deviceId);
        Instant seen = after(current, clock.instant());
        if (current.isPresent() && current.get().status() == DeviceStatus.ONLINE) {
            // If the device was silent for longer than offlineAfter, treat this as a recovery
            // (it likely powered off/on during that gap and never sent an OFFLINE/LWT)
            if (current.get().lastSeen().plus(offlineAfter).isBefore(seen)) {
                statusLogs.save(DeviceStatusLog.create(identifiers.nextDeviceStatusLogId(), deviceId,
                        DeviceStatus.ONLINE, signalStrength, seen));
                events.publish(new DeviceConnectivityRestored(deviceId, seen));
                return;
            }
            statusLogs.touch(current.get().idDeviceStatus(), signalStrength, seen);
            return;
        }
        statusLogs.save(DeviceStatusLog.create(identifiers.nextDeviceStatusLogId(), deviceId,
                DeviceStatus.ONLINE, signalStrength, seen));
        if (current.isPresent()) {
            // It was OFFLINE: the alert raised for the outage can be closed.
            events.publish(new DeviceConnectivityRestored(deviceId, seen));
        }
    }

    @Override
    public void recordOffline(String deviceId) {
        statusLogs.findLatestByDeviceId(deviceId)
                .filter(current -> current.status() == DeviceStatus.ONLINE)
                .ifPresent(current -> goOffline(current, clock.instant()));
    }

    @Override
    public int markInactiveDevicesOffline() {
        Instant now = clock.instant();
        List<DeviceStatusLog> stale = statusLogs.findOnlineNotSeenSince(now.minus(offlineAfter));
        stale.forEach(current -> goOffline(current, now));
        return stale.size();
    }

    private void goOffline(DeviceStatusLog current, Instant now) {
        Instant at = after(Optional.of(current), now);
        statusLogs.save(DeviceStatusLog.create(identifiers.nextDeviceStatusLogId(), current.deviceId(),
                DeviceStatus.OFFLINE, current.signalStrength(), at));
        events.publish(new DeviceConnectivityLost(current.deviceId(), at));
    }

    /**
     * Keeps {@code last_seen} strictly increasing per device, at the microsecond precision the
     * column stores, so the latest row is never ambiguous.
     */
    private static Instant after(Optional<DeviceStatusLog> current, Instant now) {
        Instant candidate = now.truncatedTo(ChronoUnit.MICROS);
        if (current.isEmpty() || candidate.isAfter(current.get().lastSeen())) {
            return candidate;
        }
        return current.get().lastSeen().plus(1, ChronoUnit.MICROS);
    }
}
