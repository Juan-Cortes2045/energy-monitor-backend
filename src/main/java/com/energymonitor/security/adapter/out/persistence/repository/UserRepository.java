package com.energymonitor.security.adapter.out.persistence.repository;

import com.energymonitor.security.adapter.out.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code user}.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Finds the active account whose normalised email matches.
     *
     * @param email the normalised email
     * @return the account, empty when soft-deleted or unknown
     */
    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);
}