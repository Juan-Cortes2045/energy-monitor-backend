package com.energymonitor.home.adapter.out.persistence.mapper;

import com.energymonitor.home.adapter.out.persistence.entity.HomeThresholdsEntity;
import com.energymonitor.home.domain.model.HomeThresholds;
import org.springframework.stereotype.Component;

/**
 * Converts {@link HomeThresholds} to and from {@link HomeThresholdsEntity}.
 */
@Component
public class HomeThresholdsMapper {

    public HomeThresholdsEntity toEntity(HomeThresholds thresholds) {
        HomeThresholdsEntity entity = new HomeThresholdsEntity(thresholds.idThreshold(), thresholds.homeId(),
                thresholds.dailyLimit(), thresholds.monthlyLimit(), thresholds.isUseSystemDefault());
        entity.setLimitPeriod(thresholds.limitPeriod());
        return entity;
    }

    public void applyTo(HomeThresholdsEntity entity, HomeThresholds thresholds) {
        entity.setDailyLimit(thresholds.dailyLimit());
        entity.setMonthlyLimit(thresholds.monthlyLimit());
        entity.setUseSystemDefault(thresholds.isUseSystemDefault());
        entity.setLimitPeriod(thresholds.limitPeriod());
    }

    public HomeThresholds toDomain(HomeThresholdsEntity entity) {
        return new HomeThresholds(entity.getIdThreshold(), entity.getHomeId(),
                entity.getDailyLimit(), entity.getMonthlyLimit(), entity.getUseSystemDefault(),
                entity.getLimitPeriod());
    }
}
