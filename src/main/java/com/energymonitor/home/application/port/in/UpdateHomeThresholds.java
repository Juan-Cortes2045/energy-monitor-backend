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

    /**
     * Goes back to the system default limits ({@code useSystemDefault = true}). Same access rules
     * as {@link #update}: members who cannot manage the home get 403, others 404.
     */
    HomeThresholdsResult resetToDefaults(String userId, String homeId);
}
