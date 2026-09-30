package com.energymonitor.security.adapter.out.persistence.repository;

import com.energymonitor.security.adapter.out.persistence.entity.UserSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code user_session}.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    /**
     * Finds the active session whose refresh token matches.
     *
     * @param refreshToken the opaque token value
     * @return the row, empty when soft-deleted, revoked or unknown
     */
    Optional<UserSessionEntity> findByRefreshTokenAndDeletedAtIsNull(String refreshToken);

    /**
     * Lists the active sessions of a user, most recent first.
     *
     * @param userId the owner
     * @return the sessions, newest first
     */
    List<UserSessionEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);
}