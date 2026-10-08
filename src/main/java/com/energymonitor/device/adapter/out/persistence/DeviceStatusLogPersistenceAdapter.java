package com.energymonitor.device.adapter.out.persistence;

import com.energymonitor.device.adapter.out.persistence.mapper.DeviceStatusLogMapper;
import com.energymonitor.device.adapter.out.persistence.repository.DeviceStatusLogRepository;
import com.energymonitor.device.application.port.out.DeviceStatusLogPersistencePort;
import com.energymonitor.device.domain.model.DeviceStatus;
import com.energymonitor.device.domain.model.DeviceStatusLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeviceStatusLogPersistenceAdapter implements DeviceStatusLogPersistencePort {

    private final DeviceStatusLogRepository repository;
    private final DeviceStatusLogMapper mapper;

    public DeviceStatusLogPersistenceAdapter(DeviceStatusLogRepository repository, DeviceStatusLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(DeviceStatusLog log) {
        repository.save(mapper.toEntity(log));
    }

    @Override
    public Optional<DeviceStatusLog> findLatestByDeviceId(String deviceId) {
        return repository.findFirstByDeviceIdAndDeletedAtIsNullOrderByLastSeenDescCreatedAtDesc(deviceId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void touch(String idDeviceStatus, Integer signalStrength, Instant lastSeen) {
        repository.touch(idDeviceStatus, signalStrength, lastSeen);
    }

    @Override
    public List<DeviceStatusLog> findOnlineNotSeenSince(Instant cutoff) {
        return repository.findCurrentInStatusNotSeenSince(DeviceStatus.ONLINE, cutoff).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
