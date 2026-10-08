package com.energymonitor.device.adapter.out.persistence.mapper;

import com.energymonitor.device.adapter.out.persistence.entity.DeviceStatusLogEntity;
import com.energymonitor.device.domain.model.DeviceStatusLog;
import org.springframework.stereotype.Component;

@Component
public class DeviceStatusLogMapper {

    public DeviceStatusLog toDomain(DeviceStatusLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DeviceStatusLog(
                entity.getIdDeviceStatus(),
                entity.getDeviceId(),
                entity.getStatus(),
                entity.getSignalStrength(),
                entity.getLastSeen());
    }

    public DeviceStatusLogEntity toEntity(DeviceStatusLog log) {
        if (log == null) {
            return null;
        }
        DeviceStatusLogEntity entity = new DeviceStatusLogEntity();
        entity.setIdDeviceStatus(log.idDeviceStatus());
        entity.setDeviceId(log.deviceId());
        entity.setStatus(log.status());
        entity.setSignalStrength(log.signalStrength());
        entity.setLastSeen(log.lastSeen());
        return entity;
    }
}
