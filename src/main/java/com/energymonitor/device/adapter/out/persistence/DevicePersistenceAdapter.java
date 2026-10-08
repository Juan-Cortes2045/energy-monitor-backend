package com.energymonitor.device.adapter.out.persistence;

import com.energymonitor.device.adapter.out.persistence.mapper.DeviceMapper;
import com.energymonitor.device.adapter.out.persistence.repository.DeviceRepository;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.Device;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DevicePersistenceAdapter implements DevicePersistencePort {

    private final DeviceRepository repository;
    private final DeviceMapper mapper;

    public DevicePersistenceAdapter(DeviceRepository repository, DeviceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(Device device) {
        repository.save(mapper.toEntity(device));
    }

    @Override
    public Optional<Device> findById(String deviceId) {
        return repository.findByIdDeviceAndDeletedAtIsNull(deviceId).map(mapper::toDomain);
    }

    @Override
    public Optional<Device> findByDeviceCode(String deviceCode) {
        return repository.findByDeviceCodeAndDeletedAtIsNull(deviceCode).map(mapper::toDomain);
    }
}
