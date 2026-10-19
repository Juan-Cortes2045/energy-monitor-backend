package com.energymonitor.device.application.port.out;

import com.energymonitor.device.api.DeviceConnectivityLost;
import com.energymonitor.device.api.DeviceConnectivityRestored;
import com.energymonitor.device.api.DeviceLinked;
import com.energymonitor.device.api.DeviceUnlinked;

/**
 * Publishes the events of the device module.
 */
public interface DeviceEventPort {

    void publish(DeviceConnectivityLost event);

    void publish(DeviceConnectivityRestored event);

    void publish(DeviceLinked event);

    void publish(DeviceUnlinked event);
}
