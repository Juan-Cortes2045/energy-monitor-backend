package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.port.in.DescribeHomeLimits;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import java.util.Optional;

/**
 * Answers the measurement service the limits of a home. The thresholds of a deleted home are
 * removed with it, so a deleted home has none.
 */
public class DescribeHomeLimitsService implements DescribeHomeLimits {

    private final HomeThresholdsPersistencePort thresholdsPort;

    public DescribeHomeLimitsService(HomeThresholdsPersistencePort thresholdsPort) {
        this.thresholdsPort = thresholdsPort;
    }

    @Override
    public Optional<HomeLimits> describe(String homeId) {
        if (homeId == null) {
            return Optional.empty();
        }
        return thresholdsPort.findActiveByHomeId(homeId)
                .map(t -> new HomeLimits(t.dailyLimit(), t.monthlyLimit(), t.limitPeriod()));
    }
}
