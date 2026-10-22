package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.UserHomeEntity;
import com.energymonitor.home.adapter.out.persistence.entity.UserHomeId;
import com.energymonitor.home.domain.model.Role;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code user_home}.
 */
@Repository
public interface UserHomeRepository extends JpaRepository<UserHomeEntity, UserHomeId> {

    List<UserHomeEntity> findByIdUserIdAndDeletedAtIsNull(String userId);

    List<UserHomeEntity> findByIdHomeIdAndDeletedAtIsNullOrderByCreatedAtAscIdUserIdAsc(String homeId);

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

    /**
     * Loads an active membership with {@code SELECT ... FOR UPDATE}; must run inside a transaction.
     *
     * @param id the membership key
     * @return the membership, empty when soft-deleted or missing
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from UserHomeEntity m where m.id = :id and m.deletedAt is null")
    Optional<UserHomeEntity> lockActiveById(@Param("id") UserHomeId id);
}
