package com.energymonitor.home.application.result;

import com.energymonitor.home.api.LimitPeriod;
import com.energymonitor.home.application.port.out.SystemDefaultsPort;
import com.energymonitor.home.domain.model.HomeThresholds;

/**
 * Result of a thresholds query.
 *
 * @param idThreshold         identifier
 * @param homeId              identifier of the owning home
 * @param dailyLimit          daily limit in kWh
 * @param monthlyLimit        monthly limit in kWh
 * @param useSystemDefault    whether the limits come from system defaults
 * @param limitPeriod         which limit the owner set; the other one is derived from it
 * @param defaultDailyLimit   system default daily limit in kWh
 * @param defaultMonthlyLimit system default monthly limit in kWh
 */
public record HomeThresholdsResult(String idThreshold, String homeId, double dailyLimit,
                                   double monthlyLimit, boolean useSystemDefault, LimitPeriod limitPeriod,
                                   double defaultDailyLimit, double defaultMonthlyLimit) {

    /**
     * Creates a result from a domain object.
     *
     * @param thresholds the domain object
     * @param defaults   the system defaults, shown by the client next to the switch that applies them
     * @return the result
     */
    public static HomeThresholdsResult from(HomeThresholds thresholds, SystemDefaultsPort defaults) {
        return new HomeThresholdsResult(thresholds.idThreshold(), thresholds.homeId(),
                thresholds.dailyLimit(), thresholds.monthlyLimit(), thresholds.isUseSystemDefault(),
                thresholds.limitPeriod(), defaults.defaultDailyLimit(), defaults.defaultMonthlyLimit());
    }
}
