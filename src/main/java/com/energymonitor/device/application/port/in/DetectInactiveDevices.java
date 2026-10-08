package com.energymonitor.device.application.port.in;

/**
 * Use case: mark as {@code OFFLINE} the devices that stopped reporting without a last will.
 */
public interface DetectInactiveDevices {

    /**
     * @return how many devices were marked {@code OFFLINE}
     */
    int markInactiveDevicesOffline();
}
