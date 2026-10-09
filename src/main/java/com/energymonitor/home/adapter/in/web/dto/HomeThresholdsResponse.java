package com.energymonitor.home.adapter.in.web.dto;

import com.energymonitor.home.api.LimitPeriod;

/**
 * Response DTO for home thresholds.
 *
 * @param idThreshold      identifier
 * @param homeId           identifier of the owning home
 * @param dailyLimit       daily limit in kWh
 * @param monthlyLimit     monthly limit in kWh
 * @param useSystemDefault    whether the limits come from system defaults
 * @param limitPeriod         which limit the owner set
 * @param defaultDailyLimit   system default daily limit in kWh, shown next to the defaults switch
 * @param defaultMonthlyLimit system default monthly limit in kWh
 */
public record HomeThresholdsResponse(
        String idThreshold,
        String homeId,
        double dailyLimit,
        double monthlyLimit,
        boolean useSystemDefault,
        LimitPeriod limitPeriod,
        double defaultDailyLimit,
        double defaultMonthlyLimit
) {
}
