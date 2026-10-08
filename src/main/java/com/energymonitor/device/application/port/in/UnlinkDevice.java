package com.energymonitor.device.application.port.in;

import com.energymonitor.device.application.command.UnlinkDeviceCommand;

/**
 * Use case: detach a device from a home. Its API key is rotated, so the module stops being
 * accepted by the broker; linking it again issues a new one.
 */
public interface UnlinkDevice {

    void unlink(UnlinkDeviceCommand command);
}
