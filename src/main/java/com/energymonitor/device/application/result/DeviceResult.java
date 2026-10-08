package com.energymonitor.device.application.result;

import com.energymonitor.device.domain.model.Device;
import java.time.Instant;

/**
 * Device result for application layer.
 */
public record DeviceResult(
        String idDevice,
        String name,
        String applianceTypeId,
        String location,
        String description,
        Instant installationDate,
        String deviceCode
) {

    public static DeviceResult from(Device device) {
        return new DeviceResult(
                device.idDevice(),
                device.name(),
                device.applianceTypeId(),
                device.location(),
                device.description(),
                device.installationDate(),
                device.deviceCode());
    }
}
