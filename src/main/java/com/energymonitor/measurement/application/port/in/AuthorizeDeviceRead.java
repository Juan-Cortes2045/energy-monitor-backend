package com.energymonitor.measurement.application.port.in;

/**
 * Use case: only members of the home a device is linked to may read its measurements.
 */
public interface AuthorizeDeviceRead {

    /**
     * @throws com.energymonitor.measurement.application.exception.MeasurementNotFoundException
     *         when the device is unknown, unlinked, or in a home the caller does not belong to
     */
    void requireReadable(String userId, String deviceId);
}
