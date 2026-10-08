package com.energymonitor.device.infrastructure;

import com.energymonitor.device.adapter.out.security.DeviceCredentialAdapter;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.device.api.ConnectivityStatus;
import com.energymonitor.device.application.port.in.RecordDeviceConnectivity;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.Device;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DeviceApiImpl implements DeviceApi {

    private final DevicePersistencePort devicePersistencePort;
    private final DeviceHomePersistencePort deviceHomePersistencePort;
    private final RecordDeviceConnectivity recordDeviceConnectivity;
    private final DeviceCredentialAdapter deviceCredentialAdapter;

    public DeviceApiImpl(DevicePersistencePort devicePersistencePort,
                         DeviceHomePersistencePort deviceHomePersistencePort,
                         RecordDeviceConnectivity recordDeviceConnectivity,
                         DeviceCredentialAdapter deviceCredentialAdapter) {
        this.devicePersistencePort = devicePersistencePort;
        this.deviceHomePersistencePort = deviceHomePersistencePort;
        this.recordDeviceConnectivity = recordDeviceConnectivity;
        this.deviceCredentialAdapter = deviceCredentialAdapter;
    }

    @Override
    public Optional<DeviceDto> findByDeviceId(String deviceId) {
        return devicePersistencePort.findById(deviceId).map(this::toDto);
    }

    @Override
    public Optional<DeviceDto> findByDeviceCode(String deviceCode) {
        return devicePersistencePort.findByDeviceCode(deviceCode).map(this::toDto);
    }

    @Override
    public boolean validateCredentials(String deviceCode, String apiKey) {
        return deviceCredentialAdapter.validateCredentials(deviceCode, apiKey);
    }

    @Override
    public Optional<String> getHomeId(String deviceId) {
        return deviceHomePersistencePort.findByDeviceId(deviceId).map(dh -> dh.homeId());
    }

    @Override
    public List<DeviceDto> findByHome(String homeId) {
        return deviceHomePersistencePort.findByHomeId(homeId).stream()
                .map(link -> devicePersistencePort.findById(link.deviceId()))
                .flatMap(Optional::stream)
                .map(this::toDto)
                .toList();
    }

    @Override
    public void recordConnectivity(String deviceId, ConnectivityStatus status, Integer rssi) {
        if (status == ConnectivityStatus.ONLINE) {
            recordDeviceConnectivity.recordOnline(deviceId, rssi);
        } else {
            recordDeviceConnectivity.recordOffline(deviceId);
        }
    }

    private DeviceDto toDto(Device device) {
        return new DeviceDto(
                device.idDevice(),
                device.name(),
                device.applianceTypeId(),
                device.location(),
                device.description(),
                device.installationDate(),
                device.deviceCode());
    }
}
