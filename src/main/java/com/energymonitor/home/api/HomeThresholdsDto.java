package com.energymonitor.home.api;

/**
 * DTO for exposing home thresholds to other modules.
 *
 * @param idThreshold      identifier
 * @param homeId           identifier of the owning home
 * @param dailyLimit       daily limit in kWh
 * @param monthlyLimit     monthly limit in kWh
 * @param useSystemDefault whether the limits come from system defaults
 */
public record HomeThresholdsDto(
        String idThreshold,
        String homeId,
        double dailyLimit,
        double monthlyLimit,
        boolean useSystemDefault
) {
}
