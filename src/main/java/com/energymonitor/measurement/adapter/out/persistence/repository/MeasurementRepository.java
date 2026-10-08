package com.energymonitor.measurement.adapter.out.persistence.repository;

import com.energymonitor.measurement.adapter.out.persistence.entity.MeasurementEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code measurement}.
 */
@Repository
public interface MeasurementRepository extends JpaRepository<MeasurementEntity, String> {

    List<MeasurementEntity> findByDeviceIdAndDeletedAtIsNullAndDateTimeBetweenOrderByDateTimeAsc(
            String deviceId, Instant from, Instant to);

    Optional<MeasurementEntity> findFirstByDeviceIdAndDeletedAtIsNullOrderByDateTimeDesc(String deviceId);

    Optional<MeasurementEntity> findFirstByDeviceIdAndDeletedAtIsNullAndDateTimeBeforeOrderByDateTimeDesc(
            String deviceId, Instant before);

    /**
     * One row per device and UTC hour: device_id, hour index (epoch hours), average active power,
     * minimum and maximum stored energy. UNIX_TIMESTAMP on a TIMESTAMP column yields the true
     * epoch whatever the session time zone, so the hours are UTC hours.
     */
    @Query(value = """
            SELECT device_id,
                   FLOOR(UNIX_TIMESTAMP(date_time) / 3600) AS hour_index,
                   AVG(active_power),
                   MIN(stored_energy),
                   MAX(stored_energy)
              FROM measurement
             WHERE deleted_at IS NULL
               AND device_id IN (:deviceIds)
               AND date_time >= :from
               AND date_time <= :to
             GROUP BY device_id, hour_index
             ORDER BY device_id, hour_index
            """, nativeQuery = true)
    List<Object[]> aggregateHourly(@Param("deviceIds") Collection<String> deviceIds,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);
}
