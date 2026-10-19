package com.energymonitor.home.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a home.
 *
 * @param name        display name, max 50 characters
 * @param homeTypeId  identifier of the home type
 * @param address     physical address, max 200 characters
 * @param description optional description, max 200 characters
 */
public record CreateHomeRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 10) String homeTypeId,
        @NotBlank @Size(max = 200) String address,
        @Size(max = 200) String description
) {
}
