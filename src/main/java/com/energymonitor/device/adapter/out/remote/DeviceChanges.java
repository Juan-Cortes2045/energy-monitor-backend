package com.energymonitor.device.adapter.out.remote;

/**
 * Told when the device service announces that a device joined or left a home, so what this
 * backend keeps of it is dropped at once instead of when it expires.
 */
@FunctionalInterface
public interface DeviceChanges {

    /**
     * @param deviceId the device that changed
     * @param homeId   the home it joined or left, may be null
     */
    void forget(String deviceId, String homeId);
}
