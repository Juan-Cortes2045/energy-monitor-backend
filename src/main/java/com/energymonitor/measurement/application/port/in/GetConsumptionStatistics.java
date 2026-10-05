package com.energymonitor.measurement.application.port.in;

import com.energymonitor.measurement.application.command.GetConsumptionStatisticsQuery;
import com.energymonitor.measurement.application.result.ConsumptionStatisticsResult;

/**
 * Input port for aggregating the readings of a device over a time range.
 */
public interface GetConsumptionStatistics {

    /**
     * @param query the device and optional time range
     * @return the aggregated statistics
     * @throws com.energymonitor.measurement.application.exception.MeasurementNotFoundException
     *         when the device has no measurements in the range
     */
    ConsumptionStatisticsResult get(GetConsumptionStatisticsQuery query);
}
