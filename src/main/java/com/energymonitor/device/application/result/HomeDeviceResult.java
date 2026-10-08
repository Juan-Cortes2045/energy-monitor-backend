package com.energymonitor.device.application.result;

import java.time.Instant;

/**
 * A device of a home with its current connectivity.
 *
 * @param status         {@code ONLINE}, {@code OFFLINE}, or {@code null} when it never reported
 * @param signalStrength last RSSI in dBm, may be null
 * @param lastSeen       last time the backend heard from it, may be null
 */
public record HomeDeviceResult(String idDevice, String name, String applianceTypeId, String applianceType,
                               String location, Instant installationDate, String deviceCode,
                               String status, Integer signalStrength, Instant lastSeen) {
}
