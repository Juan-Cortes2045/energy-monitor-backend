package com.energymonitor.device.api;

import java.time.Instant;

/**
 * A module was removed from its home. Its api key no longer works and the module is told to
 * offer Bluetooth again, so it can be linked right away.
 */
public record DeviceUnlinked(String deviceId, String homeId, Instant occurredAt) {
}
