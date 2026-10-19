package com.energymonitor.device.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/v1/homes/{homeId}/devices/{deviceId}}.
 */
public record UpdateDeviceRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 10) String applianceTypeId,
        @Size(max = 50) String location
) {
}
