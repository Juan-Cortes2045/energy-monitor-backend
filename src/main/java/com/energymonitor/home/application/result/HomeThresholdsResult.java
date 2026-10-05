package com.energymonitor.home.application.result;

import com.energymonitor.home.domain.model.HomeThresholds;

/**
 * Result of a thresholds query.
 *
 * @param idThreshold      identifier
 * @param homeId           identifier of the owning home
 * @param dailyLimit       daily limit in kWh
 * @param monthlyLimit     monthly limit in kWh
 * @param useSystemDefault whether the limits come from system defaults
 */
public record HomeThresholdsResult(String idThreshold, String homeId, double dailyLimit,
                                   double monthlyLimit, boolean useSystemDefault) {

    /**
     * Creates a result from a domain object.
     *
     * @param thresholds the domain object
     * @return the result
     */
    public static HomeThresholdsResult from(HomeThresholds thresholds) {
        return new HomeThresholdsResult(thresholds.idThreshold(), thresholds.homeId(),
                thresholds.dailyLimit(), thresholds.monthlyLimit(), thresholds.isUseSystemDefault());
    }
}
