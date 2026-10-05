package com.energymonitor.home.adapter.out.persistence.mapper;

import com.energymonitor.home.adapter.out.persistence.entity.UserHomeEntity;
import com.energymonitor.home.adapter.out.persistence.entity.UserHomeId;
import com.energymonitor.home.domain.model.UserHome;
import org.springframework.stereotype.Component;

/**
 * Converts {@link UserHome} to and from {@link UserHomeEntity}.
 */
@Component
public class UserHomeMapper {

    public UserHomeEntity toEntity(UserHome membership) {
        return new UserHomeEntity(
                new UserHomeId(membership.userId(), membership.homeId()),
                membership.role(),
                membership.isFavorite());
    }

    public void applyTo(UserHomeEntity entity, UserHome membership) {
        entity.setRole(membership.role());
        entity.setFavorite(membership.isFavorite());
    }

    public UserHome toDomain(UserHomeEntity entity) {
        return new UserHome(entity.getUserId(), entity.getHomeId(),
                entity.getRole(), entity.getFavorite());
    }
}
