package com.energymonitor.device.api;

import java.time.Instant;

/**
 * Domain event published when a device is registered.
 */
public record DeviceRegistered(
        String deviceId,
        String deviceCode,
        Instant occurredAt
) {
}
