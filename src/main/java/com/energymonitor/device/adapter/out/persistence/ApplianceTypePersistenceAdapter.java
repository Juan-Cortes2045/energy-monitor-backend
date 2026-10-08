package com.energymonitor.device.adapter.out.persistence;

import com.energymonitor.device.adapter.out.persistence.repository.ApplianceTypeRepository;
import com.energymonitor.device.application.port.out.ApplianceTypePersistencePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ApplianceTypePersistenceAdapter implements ApplianceTypePersistencePort {

    private final ApplianceTypeRepository repository;

    public ApplianceTypePersistenceAdapter(ApplianceTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String applianceTypeId) {
        if (applianceTypeId == null) {
            return false;
        }
        return repository.findByIdApplianceTypeAndDeletedAtIsNull(applianceTypeId).isPresent();
    }

    @Override
    public Optional<String> findIdByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return repository.findByNameAndDeletedAtIsNull(name).map(e -> e.getIdApplianceType());
    }

    @Override
    public List<ApplianceTypeEntry> findAll() {
        return repository.findByDeletedAtIsNullOrderByName().stream()
                .map(e -> new ApplianceTypeEntry(e.getIdApplianceType(), e.getName()))
                .toList();
    }
}
