package com.energymonitor.measurement.adapter.out.persistence.repository;

import com.energymonitor.measurement.adapter.out.persistence.entity.ConsumptionLevelEntity;
import com.energymonitor.measurement.api.RiskConsumption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code consumption_level}.
 */
@Repository
public interface ConsumptionLevelRepository extends JpaRepository<ConsumptionLevelEntity, String> {

    Optional<ConsumptionLevelEntity> findByNameAndDeletedAtIsNull(RiskConsumption name);

    List<ConsumptionLevelEntity> findByDeletedAtIsNull();
}
