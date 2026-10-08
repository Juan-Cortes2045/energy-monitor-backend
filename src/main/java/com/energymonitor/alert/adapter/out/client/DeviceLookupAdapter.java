package com.energymonitor.alert.adapter.out.client;

import com.energymonitor.alert.application.port.out.DeviceLookupPort;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads devices through {@code device::api}. The installation date is set again on every link,
 * so it is the moment of the latest link.
 */
@Component
public class DeviceLookupAdapter implements DeviceLookupPort {

    private final DeviceApi deviceApi;

    public DeviceLookupAdapter(DeviceApi deviceApi) {
        this.deviceApi = deviceApi;
    }

    @Override
    public Optional<Instant> linkedAt(String deviceId) {
        return deviceApi.findByDeviceId(deviceId).map(DeviceDto::installationDate);
    }

    @Override
    public List<String> deviceIdsOfHome(String homeId) {
        return deviceApi.findByHome(homeId).stream().map(DeviceDto::idDevice).toList();
    }
}
