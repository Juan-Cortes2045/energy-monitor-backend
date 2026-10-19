package com.energymonitor.measurement.adapter.out.persistence.mapper;

import com.energymonitor.measurement.adapter.out.persistence.entity.ConsumptionLevelEntity;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import org.springframework.stereotype.Component;

/**
 * Converts {@link ConsumptionLevel} to and from {@link ConsumptionLevelEntity}.
 */
@Component
public class ConsumptionLevelMapper {

    public ConsumptionLevelEntity toEntity(ConsumptionLevel level) {
        return new ConsumptionLevelEntity(level.idConsumptionLevel(), level.name(),
                level.description(), level.minLimit(), level.maxLimit());
    }

    public ConsumptionLevel toDomain(ConsumptionLevelEntity entity) {
        return new ConsumptionLevel(entity.getIdConsumptionLevel(), entity.getName(),
                entity.getDescription(), entity.getMinLimit(), entity.getMaxLimit());
    }
}
