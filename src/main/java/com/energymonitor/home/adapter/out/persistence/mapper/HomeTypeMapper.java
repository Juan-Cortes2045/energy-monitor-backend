package com.energymonitor.home.adapter.out.persistence.mapper;

import com.energymonitor.home.adapter.out.persistence.entity.HomeTypeEntity;
import com.energymonitor.home.domain.model.HomeType;
import org.springframework.stereotype.Component;

/**
 * Converts {@link HomeType} to and from {@link HomeTypeEntity}.
 */
@Component
public class HomeTypeMapper {

    public HomeTypeEntity toEntity(HomeType type) {
        return new HomeTypeEntity(type.idHomeType(), type.name());
    }

    public void applyTo(HomeTypeEntity entity, HomeType type) {
        entity.setName(type.name());
    }

    public HomeType toDomain(HomeTypeEntity entity) {
        return new HomeType(entity.getIdHomeType(), entity.getName());
    }
}
