package com.energymonitor.security.adapter.out.persistence.repository;

import com.energymonitor.security.adapter.out.persistence.entity.PasswordResetTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code password_reset_token}.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    /**
     * Finds the active token with the given value.
     *
     * @param resetToken the opaque token value
     * @return the row, empty when soft-deleted or unknown
     */
    Optional<PasswordResetTokenEntity> findByResetTokenAndDeletedAtIsNull(String resetToken);

    /**
     * Lists the active tokens issued to a user, newest first.
     *
     * <p>A user may hold several pending tokens; picking the right one to consume is the
     * caller's decision, but a deterministic order keeps it stable.
     *
     * @param userId the recipient
     * @return the pending tokens, newest first
     */
    List<PasswordResetTokenEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);
}