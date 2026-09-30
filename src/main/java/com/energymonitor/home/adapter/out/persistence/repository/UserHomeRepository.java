package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.UserHomeEntity;
import com.energymonitor.home.adapter.out.persistence.entity.UserHomeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code user_home}.
 */
@Repository
public interface UserHomeRepository extends JpaRepository<UserHomeEntity, UserHomeId> {

    List<UserHomeEntity> findByIdUserIdAndDeletedAtIsNull(String userId);

    List<UserHomeEntity> findByIdHomeIdAndDeletedAtIsNull(String homeId);

    long countByIdHomeIdAndRoleAndDeletedAtIsNull(String homeId, String role);
}
