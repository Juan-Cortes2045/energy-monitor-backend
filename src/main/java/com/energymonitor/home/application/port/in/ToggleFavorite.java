package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.ToggleFavoriteCommand;
import com.energymonitor.home.application.result.UserHomeResult;

/**
 * Input port for toggling the favorite flag of a home.
 */
public interface ToggleFavorite {

    /**
     * @param command the toggle data
     * @return the updated membership
     */
    UserHomeResult toggle(ToggleFavoriteCommand command);
}
