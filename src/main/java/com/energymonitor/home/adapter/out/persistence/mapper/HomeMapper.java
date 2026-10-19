package com.energymonitor.home.adapter.out.persistence.mapper;

import com.energymonitor.home.adapter.out.persistence.entity.HomeEntity;
import com.energymonitor.home.adapter.out.persistence.support.Instants;
import com.energymonitor.home.domain.model.Home;
import org.springframework.stereotype.Component;

/**
 * Converts {@link Home} to and from {@link HomeEntity}.
 */
@Component
public class HomeMapper {

    public HomeEntity toEntity(Home home) {
        return new HomeEntity(home.idHome(), home.name(), home.homeTypeId(), home.address(),
                home.accessCode(), home.description(), Instants.truncate(home.creationDate()));
    }

    public void applyTo(HomeEntity entity, Home home) {
        entity.setName(home.name());
        entity.setHomeTypeId(home.homeTypeId());
        entity.setAddress(home.address());
        entity.setAccessCode(home.accessCode());
        entity.setDescription(home.description());
        entity.setCreationDate(Instants.truncate(home.creationDate()));
    }

    public Home toDomain(HomeEntity entity) {
        return new Home(entity.getIdHome(), entity.getName(), entity.getHomeTypeId(),
                entity.getAddress(), entity.getAccessCode(), entity.getDescription(),
                entity.getCreationDate());
    }
}
