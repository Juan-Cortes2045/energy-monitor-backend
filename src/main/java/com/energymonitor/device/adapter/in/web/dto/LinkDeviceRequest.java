package com.energymonitor.device.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/homes/{homeId}/devices}.
 *
 * @param deviceCode code the module advertises over Bluetooth (1 to 6 letters or digits)
 */
public record LinkDeviceRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,6}") String deviceCode,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 10) String applianceTypeId,
        @Size(max = 50) String location
) {
}
