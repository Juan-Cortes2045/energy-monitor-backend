package com.energymonitor.device.api;

import java.time.Instant;

/**
 * Public DTO of a device.
 */
public record DeviceDto(
        String idDevice,
        String name,
        String applianceTypeId,
        String location,
        String description,
        Instant installationDate,
        String deviceCode
) {
}
