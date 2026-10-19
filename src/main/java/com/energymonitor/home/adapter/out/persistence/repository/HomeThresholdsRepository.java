package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.HomeThresholdsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code home_thresholds}.
 */
@Repository
public interface HomeThresholdsRepository extends JpaRepository<HomeThresholdsEntity, String> {

    Optional<HomeThresholdsEntity> findByHomeIdAndDeletedAtIsNull(String homeId);
}
