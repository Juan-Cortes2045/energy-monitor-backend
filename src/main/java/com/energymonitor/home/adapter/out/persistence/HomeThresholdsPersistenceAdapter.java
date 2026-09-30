package com.energymonitor.home.adapter.out.persistence;

import com.energymonitor.home.adapter.out.persistence.entity.HomeThresholdsEntity;
import com.energymonitor.home.adapter.out.persistence.mapper.HomeThresholdsMapper;
import com.energymonitor.home.adapter.out.persistence.repository.HomeThresholdsRepository;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.domain.model.HomeThresholds;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@link HomeThresholds}.
 */
@Component
public class HomeThresholdsPersistenceAdapter implements HomeThresholdsPersistencePort {

    private final HomeThresholdsRepository repository;
    private final HomeThresholdsMapper mapper;

    public HomeThresholdsPersistenceAdapter(HomeThresholdsRepository repository,
                                           HomeThresholdsMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public HomeThresholds save(HomeThresholds thresholds) {
        HomeThresholdsEntity entity = repository.findById(thresholds.idThreshold())
                .map(existing -> {
                    mapper.applyTo(existing, thresholds);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(thresholds));
        repository.save(entity);
        return thresholds;
    }

    @Override
    public Optional<HomeThresholds> findActiveByHomeId(String homeId) {
        return repository.findByHomeIdAndDeletedAtIsNull(homeId)
                .map(mapper::toDomain);
    }
}
