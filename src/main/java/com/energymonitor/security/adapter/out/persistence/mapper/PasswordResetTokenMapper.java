package com.energymonitor.security.adapter.out.persistence.mapper;

import com.energymonitor.security.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.energymonitor.security.adapter.out.persistence.support.Instants;
import com.energymonitor.security.domain.model.PasswordResetToken;
import org.springframework.stereotype.Component;

/**
 * Converts {@link PasswordResetToken} to and from {@link PasswordResetTokenEntity}.
 *
 * <p>{@code used} is the whole point of the mapping: it must survive the round trip, or a
 * consumed token would come back looking redeemable. The rehydration constructor is the one
 * that takes {@code used} explicitly, and this mapper preserves the flag as stored.
 *
 * <p>The issue and expiry instants are truncated to whole seconds, matching the column
 * precision, so a stored token rehydrates without a rotation of nanoseconds.
 */
@Component
public class PasswordResetTokenMapper {

    /**
     * Builds a new entity from the domain object.
     *
     * @param token the domain object
     * @return a detached entity ready to be persisted
     */
    public PasswordResetTokenEntity toEntity(PasswordResetToken token) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setIdResetToken(token.idResetToken());
        entity.setUserId(token.idUser());
        entity.setResetToken(token.resetToken());
        entity.setUsed(token.isUsed());
        entity.setExpirationAt(Instants.truncate(token.expirationAt()));
        entity.setCreatedAt(Instants.truncate(token.createdAt()));
        return entity;
    }

    /**
     * Copies the domain state onto an already managed entity, for updates.
     *
     * <p>{@code created_at} is not touched here: the entity keeps the issue instant that was
     * stored on insert, and {@code updated_at} is refreshed by the audit callback.
     *
     * @param entity the managed entity
     * @param token  the domain object holding the new state
     */
    public void applyTo(PasswordResetTokenEntity entity, PasswordResetToken token) {
        entity.setUserId(token.idUser());
        entity.setResetToken(token.resetToken());
        entity.setUsed(token.isUsed());
        entity.setExpirationAt(Instants.truncate(token.expirationAt()));
    }

    /**
     * Rehydrates the domain object from a stored row.
     *
     * @param entity the stored row
     * @return the domain object
     */
    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return new PasswordResetToken(
                entity.getIdResetToken(),
                entity.getUserId(),
                entity.getResetToken(),
                entity.getCreatedAt(),
                entity.getExpirationAt(),
                entity.isUsed());
    }
}