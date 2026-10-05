package com.energymonitor.home.adapter.in.web.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request DTO for updating home thresholds.
 *
 * @param dailyLimit   daily limit in kWh, must be positive
 * @param monthlyLimit monthly limit in kWh, must be positive
 */
public record UpdateHomeThresholdsRequest(
        @Positive double dailyLimit,
        @Positive double monthlyLimit
) {
}
