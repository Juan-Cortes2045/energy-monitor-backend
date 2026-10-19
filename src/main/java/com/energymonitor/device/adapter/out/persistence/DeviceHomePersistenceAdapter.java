package com.energymonitor.device.adapter.out.persistence;

import com.energymonitor.device.adapter.out.persistence.mapper.DeviceHomeMapper;
import com.energymonitor.device.adapter.out.persistence.repository.DeviceHomeRepository;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.domain.model.DeviceHome;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeviceHomePersistenceAdapter implements DeviceHomePersistencePort {

    private final DeviceHomeRepository repository;
    private final DeviceHomeMapper mapper;

    public DeviceHomePersistenceAdapter(DeviceHomeRepository repository, DeviceHomeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(DeviceHome deviceHome) {
        // The mapped entity has no deleted_at, so saving a pair that was unlinked before
        // merges into the old row and revives it.
        repository.save(mapper.toEntity(deviceHome));
    }

    @Override
    public Optional<DeviceHome> findByDeviceId(String deviceId) {
        return repository.findByDeviceIdAndDeletedAtIsNull(deviceId).map(mapper::toDomain);
    }

    @Override
    public List<DeviceHome> findByHomeId(String homeId) {
        return repository.findByHomeIdAndDeletedAtIsNull(homeId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void unlink(String deviceId) {
        repository.unlink(deviceId, Instant.now());
    }
}
