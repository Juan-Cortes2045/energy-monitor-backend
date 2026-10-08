package com.energymonitor.device.adapter.out.persistence.repository;

import com.energymonitor.device.adapter.out.persistence.entity.DeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {

    Optional<DeviceEntity> findByIdDeviceAndDeletedAtIsNull(String idDevice);

    Optional<DeviceEntity> findByDeviceCodeAndDeletedAtIsNull(String deviceCode);
}
