package com.energymonitor.alert.application.port.out;

import java.util.Optional;

/**
 * Output port for resolving the home a device belongs to.
 *
 * <p>The {@code DeviceHome} relationship lives in the Devices bounded context. Until that
 * module exposes its public API, the adapter is a placeholder that resolves nothing, and
 * threshold alerts are skipped rather than raised against a guessed home.
 */
public interface HomeLookupPort {

    /**
     * @param deviceId the device identifier
     * @return the home identifier, empty when the device cannot be resolved
     */
    Optional<String> findHomeIdByDeviceId(String deviceId);
}
