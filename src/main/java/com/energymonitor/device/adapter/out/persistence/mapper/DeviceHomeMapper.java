package com.energymonitor.device.adapter.out.persistence.mapper;

import com.energymonitor.device.adapter.out.persistence.entity.DeviceHomeEntity;
import com.energymonitor.device.adapter.out.persistence.entity.DeviceHomeId;
import com.energymonitor.device.domain.model.DeviceHome;
import org.springframework.stereotype.Component;

@Component
public class DeviceHomeMapper {

    public DeviceHome toDomain(DeviceHomeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DeviceHome(entity.getDeviceId(), entity.getHomeId());
    }

    public DeviceHomeEntity toEntity(DeviceHome deviceHome) {
        if (deviceHome == null) {
            return null;
        }
        DeviceHomeEntity entity = new DeviceHomeEntity();
        entity.setId(new DeviceHomeId(deviceHome.deviceId(), deviceHome.homeId()));
        entity.setDeviceId(deviceHome.deviceId());
        entity.setHomeId(deviceHome.homeId());
        return entity;
    }
}
