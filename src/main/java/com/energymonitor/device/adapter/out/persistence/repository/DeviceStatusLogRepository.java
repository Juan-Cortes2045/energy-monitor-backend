package com.energymonitor.device.adapter.out.persistence.repository;

import com.energymonitor.device.adapter.out.persistence.entity.DeviceStatusLogEntity;
import com.energymonitor.device.domain.model.DeviceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceStatusLogRepository extends JpaRepository<DeviceStatusLogEntity, String> {

    Optional<DeviceStatusLogEntity> findFirstByDeviceIdAndDeletedAtIsNullOrderByLastSeenDescCreatedAtDesc(
            String deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DeviceStatusLogEntity l
               set l.signalStrength = :signalStrength, l.lastSeen = :lastSeen
             where l.idDeviceStatus = :id
            """)
    void touch(@Param("id") String id,
               @Param("signalStrength") Integer signalStrength,
               @Param("lastSeen") Instant lastSeen);

    /**
     * Logs in {@code status} not refreshed since {@code cutoff} that are still the latest of their
     * device. A newer log with the same {@code last_seen} also counts as newer, so two rows written
     * in the same second never both look current.
     */
    @Query("""
            select l from DeviceStatusLogEntity l
             where l.status = :status
               and l.lastSeen < :cutoff
               and l.deletedAt is null
               and not exists (
                   select n from DeviceStatusLogEntity n
                    where n.deviceId = l.deviceId
                      and n.idDeviceStatus <> l.idDeviceStatus
                      and n.deletedAt is null
                      and n.lastSeen >= l.lastSeen)
            """)
    List<DeviceStatusLogEntity> findCurrentInStatusNotSeenSince(@Param("status") DeviceStatus status,
                                                                @Param("cutoff") Instant cutoff);
}
