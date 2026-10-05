package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.LeaveHomeCommand;

/**
 * Input port for leaving a home.
 */
public interface LeaveHome {

    /**
     * @param command the leave data
     */
    void leave(LeaveHomeCommand command);
}
