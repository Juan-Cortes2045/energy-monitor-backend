package com.energymonitor.measurement.adapter.out.persistence;

import com.energymonitor.measurement.adapter.out.persistence.mapper.ConsumptionLevelMapper;
import com.energymonitor.measurement.adapter.out.persistence.repository.ConsumptionLevelRepository;
import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.port.out.ConsumptionLevelPersistencePort;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for the {@link ConsumptionLevel} catalog.
 *
 * <p>{@link #findLevelFor(double)} classifies in memory: the catalog holds four rows, so a
 * range query in SQL would buy nothing over filtering the cached list.
 */
@Component
public class ConsumptionLevelPersistenceAdapter implements ConsumptionLevelPersistencePort {

    private final ConsumptionLevelRepository repository;
    private final ConsumptionLevelMapper mapper;

    public ConsumptionLevelPersistenceAdapter(ConsumptionLevelRepository repository,
                                              ConsumptionLevelMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ConsumptionLevel> findActiveByName(RiskConsumption name) {
        return repository.findByNameAndDeletedAtIsNull(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<ConsumptionLevel> listActive() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ConsumptionLevel> findLevelFor(double activePower) {
        return listActive().stream()
                .filter(level -> level.contains(activePower))
                .findFirst();
    }
}
