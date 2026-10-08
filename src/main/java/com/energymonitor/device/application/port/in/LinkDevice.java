package com.energymonitor.device.application.port.in;

import com.energymonitor.device.application.command.LinkDeviceCommand;
import com.energymonitor.device.application.result.LinkedDeviceResult;

/**
 * Use case: link a module to a home and issue its credentials.
 *
 * <p>A code seen for the first time creates the device. A code already known is the same
 * module being set up again (new Wi-Fi, a factory reset, a failed first attempt): it keeps its
 * identity and gets a new API key, provided it is not linked to another home.
 */
public interface LinkDevice {

    LinkedDeviceResult link(LinkDeviceCommand command);
}
