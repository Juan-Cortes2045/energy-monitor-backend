package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.HomeEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code home}.
 */
@Repository
public interface HomeRepository extends JpaRepository<HomeEntity, String> {

    Optional<HomeEntity> findByAccessCodeAndDeletedAtIsNull(String accessCode);

    List<HomeEntity> findByIdHomeInAndDeletedAtIsNull(Collection<String> ids);
}
