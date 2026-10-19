package com.energymonitor.device.application.port.in;

import com.energymonitor.device.application.result.HomeDeviceResult;
import java.util.List;

/**
 * Use case: the devices of a home, for any member of it.
 */
public interface ListHomeDevices {

    List<HomeDeviceResult> list(String userId, String homeId);
}
