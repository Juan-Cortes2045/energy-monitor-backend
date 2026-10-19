package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.JoinHomeCommand;
import com.energymonitor.home.application.result.UserHomeResult;

/**
 * Input port for joining a home with an access code. The user becomes a MEMBER.
 */
public interface JoinHome {

    /**
     * @param command the join data
     * @return the new membership
     */
    UserHomeResult join(JoinHomeCommand command);
}
