package com.energymonitor.home.adapter.out.persistence;

import com.energymonitor.home.adapter.out.persistence.entity.HomeTypeEntity;
import com.energymonitor.home.adapter.out.persistence.mapper.HomeTypeMapper;
import com.energymonitor.home.adapter.out.persistence.repository.HomeTypeRepository;
import com.energymonitor.home.application.port.out.HomeTypePersistencePort;
import com.energymonitor.home.domain.model.HomeType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@link HomeType}.
 */
@Component
public class HomeTypePersistenceAdapter implements HomeTypePersistencePort {

    private final HomeTypeRepository repository;
    private final HomeTypeMapper mapper;

    public HomeTypePersistenceAdapter(HomeTypeRepository repository, HomeTypeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<HomeType> findActive(String idHomeType) {
        return repository.findById(idHomeType)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<HomeType> findAllActive() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
