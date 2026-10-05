package com.energymonitor.home.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for joining a home with an access code.
 *
 * @param accessCode the 8-character join code
 */
public record JoinHomeRequest(
        @NotBlank @Size(max = 8) String accessCode
) {
}
