package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.CreateHomeCommand;
import com.energymonitor.home.application.result.HomeResult;

/**
 * Input port for creating a home. The creator becomes the OWNER.
 */
public interface CreateHome {

    /**
     * @param command the creation data
     * @return the created home
     */
    HomeResult create(CreateHomeCommand command);
}
