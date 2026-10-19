package com.energymonitor.device.application.port.in;

import com.energymonitor.device.application.command.UpdateDeviceCommand;
import com.energymonitor.device.application.result.HomeDeviceResult;

/**
 * Use case: change what a linked device measures, its name or its room. Only an OWNER.
 */
public interface UpdateDevice {

    HomeDeviceResult update(UpdateDeviceCommand command);
}
