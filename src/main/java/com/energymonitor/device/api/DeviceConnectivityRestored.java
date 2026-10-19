package com.energymonitor.device.api;

import java.time.Instant;

/**
 * Domain event published when a device that was offline reports again.
 */
public record DeviceConnectivityRestored(
        String deviceId,
        Instant occurredAt
) {
}
