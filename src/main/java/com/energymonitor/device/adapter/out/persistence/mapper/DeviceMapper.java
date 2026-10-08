package com.energymonitor.device.adapter.out.persistence.mapper;

import com.energymonitor.device.adapter.out.persistence.entity.DeviceEntity;
import com.energymonitor.device.domain.model.Device;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public Device toDomain(DeviceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Device(
                entity.getIdDevice(),
                entity.getName(),
                entity.getApplianceTypeId(),
                entity.getLocation(),
                entity.getDescription(),
                entity.getInstallationDate(),
                entity.getDeviceCode(),
                entity.getApiKey());
    }

    public DeviceEntity toEntity(Device device) {
        if (device == null) {
            return null;
        }
        DeviceEntity entity = new DeviceEntity();
        entity.setIdDevice(device.idDevice());
        entity.setName(device.name());
        entity.setApplianceTypeId(device.applianceTypeId());
        entity.setLocation(device.location());
        entity.setDescription(device.description());
        entity.setInstallationDate(device.installationDate());
        entity.setDeviceCode(device.deviceCode());
        entity.setApiKey(device.apiKey());
        return entity;
    }
}
