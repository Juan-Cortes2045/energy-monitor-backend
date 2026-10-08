package com.energymonitor.device.application.result;

/**
 * Outcome of linking a module. {@code apiKey} is returned only here: the caller hands it to
 * the module over Bluetooth and it is never shown again.
 */
public record LinkedDeviceResult(HomeDeviceResult device, String apiKey) {
}
