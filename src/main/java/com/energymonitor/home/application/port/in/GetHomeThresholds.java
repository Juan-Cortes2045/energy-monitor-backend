package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.GetHomeThresholdsQuery;
import com.energymonitor.home.application.result.HomeThresholdsResult;

/**
 * Input port for retrieving the thresholds of a home.
 */
public interface GetHomeThresholds {

    /**
     * @param query the query data
     * @return the thresholds
     */
    HomeThresholdsResult get(GetHomeThresholdsQuery query);
}
