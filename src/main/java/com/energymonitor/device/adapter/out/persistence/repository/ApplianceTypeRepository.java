package com.energymonitor.device.adapter.out.persistence.repository;

import com.energymonitor.device.adapter.out.persistence.entity.ApplianceTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplianceTypeRepository extends JpaRepository<ApplianceTypeEntity, String> {

    Optional<ApplianceTypeEntity> findByIdApplianceTypeAndDeletedAtIsNull(String id);

    Optional<ApplianceTypeEntity> findByNameAndDeletedAtIsNull(String name);

    List<ApplianceTypeEntity> findByDeletedAtIsNullOrderByName();
}
