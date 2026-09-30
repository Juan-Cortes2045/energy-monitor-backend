package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.HomeTypeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code home_type}.
 */
@Repository
public interface HomeTypeRepository extends JpaRepository<HomeTypeEntity, String> {

    List<HomeTypeEntity> findByDeletedAtIsNull();
}
