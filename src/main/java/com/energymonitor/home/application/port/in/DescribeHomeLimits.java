package com.energymonitor.home.application.port.in;

import com.energymonitor.home.api.LimitPeriod;
import java.util.Optional;

/**
 * Input port for the measurement service asking the consumption limits of a home, to show how
 * much of them the home has used.
 */
public interface DescribeHomeLimits {

    /**
     * @param homeId the home in the path of the consumption request
     * @return its limits, empty when the home has none (deleted, or never configured)
     */
    Optional<HomeLimits> describe(String homeId);

    /**
     * The limits of a home.
     *
     * @param dailyLimit   daily limit in kWh
     * @param monthlyLimit monthly limit in kWh
     * @param limitPeriod  which of the two the owner set; the other is derived from it
     */
    record HomeLimits(double dailyLimit, double monthlyLimit, LimitPeriod limitPeriod) {
    }
}
