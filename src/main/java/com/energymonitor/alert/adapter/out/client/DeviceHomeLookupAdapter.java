package com.energymonitor.alert.adapter.out.client;

import com.energymonitor.alert.application.port.out.HomeLookupPort;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.home.api.HomeApi;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves homes through the public APIs of the device and home modules.
 */
@Component
public class DeviceHomeLookupAdapter implements HomeLookupPort {

    private final DeviceApi deviceApi;
    private final HomeApi homeApi;

    public DeviceHomeLookupAdapter(DeviceApi deviceApi, HomeApi homeApi) {
        this.deviceApi = deviceApi;
        this.homeApi = homeApi;
    }

    @Override
    public Optional<String> findHomeIdByDeviceId(String deviceId) {
        return deviceApi.getHomeId(deviceId);
    }

    @Override
    public boolean isMember(String userId, String homeId) {
        return userId != null && homeId != null && homeApi.isMember(userId, homeId);
    }
}
