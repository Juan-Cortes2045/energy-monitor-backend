package com.energymonitor.recommendation.adapter.out.client;

import com.energymonitor.recommendation.application.port.out.HomeReaderPort;
import com.energymonitor.recommendation.domain.model.ConsumptionLimit;
import com.energymonitor.recommendation.domain.model.MonitoredDevice;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.home.api.HomeApi;
import com.energymonitor.home.api.LimitPeriod;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads homes, members, devices and limits through {@code home::api} and {@code device::api}.
 */
@Component
public class HomeDeviceReaderAdapter implements HomeReaderPort {

    /** Appliance type seeded as {@code refrigerator} (device-006): it runs all night by design. */
    static final String REFRIGERATOR_TYPE_ID = "appl000001";

    private final HomeApi homeApi;
    private final DeviceApi deviceApi;

    public HomeDeviceReaderAdapter(HomeApi homeApi, DeviceApi deviceApi) {
        this.homeApi = homeApi;
        this.deviceApi = deviceApi;
    }

    @Override
    public Optional<String> homeOfDevice(String deviceId) {
        return deviceApi.getHomeId(deviceId);
    }

    @Override
    public List<MonitoredDevice> devicesOf(String homeId) {
        return deviceApi.findByHome(homeId).stream()
                .map(d -> new MonitoredDevice(d.idDevice(), REFRIGERATOR_TYPE_ID.equals(d.applianceTypeId())))
                .toList();
    }

    @Override
    public Optional<ConsumptionLimit> limitOf(String homeId) {
        return homeApi.findThresholds(homeId).map(t -> t.limitPeriod() == LimitPeriod.MONTHLY
                ? new ConsumptionLimit(ConsumptionLimit.Period.MONTHLY, t.monthlyLimit())
                : new ConsumptionLimit(ConsumptionLimit.Period.DAILY, t.dailyLimit()));
    }

    @Override
    public boolean isMember(String userId, String homeId) {
        return userId != null && homeId != null && homeApi.isMember(userId, homeId);
    }

    @Override
    public Optional<String> deviceName(String deviceId) {
        return deviceApi.findByDeviceId(deviceId).map(DeviceDto::name);
    }
}
