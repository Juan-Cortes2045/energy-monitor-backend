package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.RemoveUserCommand;

/**
 * Input port for removing a user from a home.
 */
public interface RemoveUser {

    /**
     * @param command the removal data
     */
    void remove(RemoveUserCommand command);
}
