package com.energymonitor.measurement.adapter.out.persistence.repository;

import com.energymonitor.measurement.adapter.out.persistence.entity.MeasurementEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code measurement}.
 */
@Repository
public interface MeasurementRepository extends JpaRepository<MeasurementEntity, String> {

    List<MeasurementEntity> findByDeviceIdAndDeletedAtIsNullAndDateTimeBetweenOrderByDateTimeAsc(
            String deviceId, Instant from, Instant to);

    Optional<MeasurementEntity> findFirstByDeviceIdAndDeletedAtIsNullOrderByDateTimeDesc(String deviceId);
}
