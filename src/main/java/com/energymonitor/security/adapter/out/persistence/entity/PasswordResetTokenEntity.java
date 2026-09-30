package com.energymonitor.security.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA mapping of the {@code password_reset_token} table.
 *
 * <p>{@code used} is stored as a real column and never inferred. Rehydration has to hand the
 * domain the flag exactly as persisted, otherwise a consumed token would come back looking
 * fresh and could be redeemed twice.
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetTokenEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_reset_token", nullable = false, length = 10)
    private String idResetToken;

    @Column(name = "user_id", nullable = false, length = 10)
    private String userId;

    @Column(name = "reset_token", nullable = false, length = 100)
    private String resetToken;

    @Column(name = "used", nullable = false)
    @TinyIntBoolean
    private boolean used;

    @Column(name = "expiration_at", nullable = false)
    private Instant expirationAt;

    public PasswordResetTokenEntity() {
    }

    public String getIdResetToken() {
        return idResetToken;
    }

    public void setIdResetToken(String idResetToken) {
        this.idResetToken = idResetToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /** @return expiry instant, read from the column named in the changelog */
    public Instant getExpirationAt() {
        return expirationAt;
    }

    public void setExpirationAt(Instant expirationAt) {
        this.expirationAt = expirationAt;
    }
}
