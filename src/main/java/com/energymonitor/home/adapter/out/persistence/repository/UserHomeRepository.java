package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.UserHomeEntity;
import com.energymonitor.home.adapter.out.persistence.entity.UserHomeId;
import com.energymonitor.home.domain.model.Role;
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

    /**
     * The role is bound as {@link Role} because that is how the entity stores it. Declaring it as
     * {@code String} let Hibernate reject every call, because a literal such as {@code "OWNER"} is
     * not assignable to the enum the column holds.
     *
     * @param homeId the home to count owners in
     * @param role   the role to count
     * @return how many members hold that role and have not been removed
     */
    long countByIdHomeIdAndRoleAndDeletedAtIsNull(String homeId, Role role);
}
