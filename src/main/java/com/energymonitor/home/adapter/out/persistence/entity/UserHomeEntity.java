package com.energymonitor.home.adapter.out.persistence.entity;

import com.energymonitor.home.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping of the {@code user_home} table.
 *
 * <p>The identifier is the {@code (user_id, home_id)} pair. Because the pair is the primary key,
 * a soft-deleted row still occupies it. The reactivation strategy needs to load a deleted row
 * in order to clear {@code deleted_at} and revive it.
 */
@Entity
@Table(name = "user_home")
public class UserHomeEntity extends BaseAuditEntity {

    @EmbeddedId
    private UserHomeId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "favorite", nullable = false)
    private Boolean favorite;

    public UserHomeEntity() {
    }

    public UserHomeEntity(UserHomeId id, Role role, Boolean favorite) {
        this.id = id;
        this.role = role;
        this.favorite = favorite;
    }

    public UserHomeId getId() {
        return id;
    }

    public void setId(UserHomeId id) {
        this.id = id;
    }

    public String getUserId() {
        return id == null ? null : id.getUserId();
    }

    public String getHomeId() {
        return id == null ? null : id.getHomeId();
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }
}
