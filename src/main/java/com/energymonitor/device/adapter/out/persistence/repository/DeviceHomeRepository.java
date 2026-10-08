package com.energymonitor.device.adapter.out.persistence.repository;

import com.energymonitor.device.adapter.out.persistence.entity.DeviceHomeEntity;
import com.energymonitor.device.adapter.out.persistence.entity.DeviceHomeId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceHomeRepository extends JpaRepository<DeviceHomeEntity, DeviceHomeId> {

    Optional<DeviceHomeEntity> findByDeviceIdAndDeletedAtIsNull(String deviceId);

    List<DeviceHomeEntity> findByHomeIdAndDeletedAtIsNull(String homeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DeviceHomeEntity dh set dh.deletedAt = :now where dh.deviceId = :deviceId and dh.deletedAt is null")
    void unlink(@Param("deviceId") String deviceId, @Param("now") Instant now);
}
