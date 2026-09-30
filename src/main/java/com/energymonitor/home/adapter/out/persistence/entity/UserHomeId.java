package com.energymonitor.home.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Primary key of the {@code user_home} table: {@code (user_id, home_id)}.
 */
@Embeddable
public class UserHomeId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 10)
    private String userId;

    @Column(name = "home_id", nullable = false, length = 10)
    private String homeId;

    public UserHomeId() {
    }

    public UserHomeId(String userId, String homeId) {
        this.userId = userId;
        this.homeId = homeId;
    }

    public String getUserId() {
        return userId;
    }

    public String getHomeId() {
        return homeId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserHomeId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(homeId, that.homeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, homeId);
    }

    @Override
    public String toString() {
        return "UserHomeId{userId='" + userId + "', homeId='" + homeId + "'}";
    }
}
