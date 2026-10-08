package com.energymonitor.recommendation.adapter.out.persistence.entity;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * JPA mapping of the {@code recommendation} table. {@code home_id} and {@code device_id} have no
 * SQL foreign keys: they reference other bounded contexts at application level.
 */
@Entity
@Table(name = "recommendation")
public class RecommendationEntity {

    @Id
    @Column(name = "id_recommendation", nullable = false, length = 10)
    private String idRecommendation;

    @Column(name = "home_id", nullable = false, length = 10)
    private String homeId;

    @Column(name = "device_id", length = 10)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 21)
    private RecommendationType type;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(name = "date_time", nullable = false)
    private Instant dateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 6)
    private RecommendationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public RecommendationEntity() {
    }

    public RecommendationEntity(String idRecommendation, String homeId, String deviceId,
                                RecommendationType type, String messageKey, Instant dateTime,
                                RecommendationStatus status) {
        this.idRecommendation = idRecommendation;
        this.homeId = homeId;
        this.deviceId = deviceId;
        this.type = type;
        this.messageKey = messageKey;
        this.dateTime = dateTime;
        this.status = status;
    }

    @PrePersist
    void onPersist() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public String getIdRecommendation() {
        return idRecommendation;
    }

    public String getHomeId() {
        return homeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public RecommendationType getType() {
        return type;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Instant getDateTime() {
        return dateTime;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
