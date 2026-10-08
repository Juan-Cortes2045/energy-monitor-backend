package com.energymonitor.device.application.port.in;

import com.energymonitor.device.application.result.LinkedDeviceResult;

/**
 * Use case: a new API key for a module already linked to the home, so the web app can write it
 * again over Bluetooth together with a new Wi-Fi network (moving house, a new password, a new
 * internet provider). Only an OWNER. It is not a new link: the link date stays and no
 * "device linked" alert follows.
 */
public interface ReissueDeviceCredentials {

    LinkedDeviceResult reissue(String userId, String homeId, String deviceId);
}
