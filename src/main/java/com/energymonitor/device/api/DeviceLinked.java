package com.energymonitor.device.api;

import java.time.Instant;

/**
 * Domain event published when a module is linked to a home from the web, the first time or
 * again (new network, new key).
 *
 * @param deviceId   the device
 * @param homeId     the home it is now linked to
 * @param deviceName display name chosen when linking
 * @param linkedBy   the owner who linked it
 */
public record DeviceLinked(String deviceId, String homeId, String deviceName, String linkedBy,
                           Instant occurredAt) {
}
