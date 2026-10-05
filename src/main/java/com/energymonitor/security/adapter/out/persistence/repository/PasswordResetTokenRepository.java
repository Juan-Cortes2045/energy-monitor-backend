package com.energymonitor.security.adapter.out.persistence.repository;

import com.energymonitor.security.adapter.out.persistence.entity.PasswordResetTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code password_reset_token}.
 *
 * <p>Every lookup is by hash. There is deliberately no finder taking a raw secret, so there is
 * no code path that could query the table with a usable credential, the same rule
 * {@code RefreshTokenRepository} follows.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    /**
     * Finds the active token whose stored hash matches.
     *
     * @param resetTokenHash the hash of the secret to resolve
     * @return the row, empty when soft-deleted or unknown
     */
    Optional<PasswordResetTokenEntity> findByResetTokenHashAndDeletedAtIsNull(String resetTokenHash);

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