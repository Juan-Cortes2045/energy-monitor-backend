package com.energymonitor.measurement.application.port.in;

import com.energymonitor.measurement.application.result.HomeConsumptionHistoryResult;
import com.energymonitor.measurement.application.result.HomeConsumptionSummaryResult;
import java.time.ZoneId;

/**
 * Use case: the consumption of a home, for any of its members. Day and month boundaries are
 * those of {@code zone}, the caller's time zone.
 */
public interface GetHomeConsumption {

    enum Period {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    HomeConsumptionSummaryResult summary(String userId, String homeId, ZoneId zone);

    HomeConsumptionHistoryResult history(String userId, String homeId, Period period, ZoneId zone);
}
