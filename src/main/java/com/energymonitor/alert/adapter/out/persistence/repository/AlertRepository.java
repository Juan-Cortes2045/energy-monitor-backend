package com.energymonitor.alert.adapter.out.persistence.repository;

import com.energymonitor.alert.adapter.out.persistence.entity.AlertEntity;
import com.energymonitor.alert.api.AlertStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code alert}.
 */
@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, String> {

    List<AlertEntity> findByHomeIdAndDeletedAtIsNullOrderByDateTimeDesc(String homeId);

    List<AlertEntity> findByHomeIdAndAlertStatusAndDeletedAtIsNullOrderByDateTimeDesc(
            String homeId, AlertStatus status);
}
