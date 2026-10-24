package com.energymonitor.device.api;

import java.util.List;
import java.util.Optional;

/**
 * Devices as the other modules of this backend read them. They live in the device service, which
 * answers through its internal API; see {@code RemoteDeviceApi}.
 */
public interface DeviceApi {

    /**
     * @param deviceId the device
     * @return the device, empty when it does not exist
     * @throws DeviceServiceUnavailableException when the service does not answer and nothing was
     *                                           kept from an earlier answer
     */
    Optional<DeviceDto> findByDeviceId(String deviceId);

    /**
     * @param deviceId the device
     * @return the home it is linked to, empty when it does not exist or is linked to none
     * @throws DeviceServiceUnavailableException as {@link #findByDeviceId}
     */
    Optional<String> getHomeId(String deviceId);

    /**
     * The devices currently linked to a home.
     *
     * @throws DeviceServiceUnavailableException as {@link #findByDeviceId}
     */
    List<DeviceDto> findByHome(String homeId);
}
