package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.UpdateHomeThresholdsCommand;
import com.energymonitor.home.application.result.HomeThresholdsResult;

/**
 * Input port for updating the thresholds of a home.
 */
public interface UpdateHomeThresholds {

    /**
     * @param command the update data
     * @return the updated thresholds
     */
    HomeThresholdsResult update(UpdateHomeThresholdsCommand command);
}
