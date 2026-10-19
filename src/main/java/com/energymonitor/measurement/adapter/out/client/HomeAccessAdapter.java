package com.energymonitor.measurement.adapter.out.client;

import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.home.api.HomeApi;
import com.energymonitor.measurement.application.port.out.HomeAccessPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads homes and device links through the public APIs of the home and device modules.
 */
@Component
public class HomeAccessAdapter implements HomeAccessPort {

    private final HomeApi homeApi;
    private final DeviceApi deviceApi;

    public HomeAccessAdapter(HomeApi homeApi, DeviceApi deviceApi) {
        this.homeApi = homeApi;
        this.deviceApi = deviceApi;
    }

    @Override
    public boolean isMember(String userId, String homeId) {
        return userId != null && homeId != null && homeApi.homeExists(homeId) && homeApi.isMember(userId, homeId);
    }

    @Override
    public List<String> deviceIds(String homeId) {
        return deviceApi.findByHome(homeId).stream().map(DeviceDto::idDevice).toList();
    }

    @Override
    public Optional<String> homeOf(String deviceId) {
        return deviceApi.getHomeId(deviceId);
    }

    @Override
    public Optional<Limits> limits(String homeId) {
        return homeApi.findThresholds(homeId).map(t -> new Limits(t.dailyLimit(), t.monthlyLimit(), t.limitPeriod().name()));
    }
}
