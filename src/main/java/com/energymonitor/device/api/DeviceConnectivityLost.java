package com.energymonitor.device.api;

import java.time.Instant;

/**
 * Domain event published when a device connectivity is lost.
 */
public record DeviceConnectivityLost(
        String deviceId,
        Instant occurredAt
) {
}
