package com.energymonitor.recommendation.domain.model;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root of a recommendation: a suggestion derived from the observed consumption of a
 * home. Maps to the {@code recommendation} table.
 *
 * <p>{@code homeId} and {@code deviceId} reference other bounded contexts at application level
 * only. {@code deviceId} is {@code null} when the suggestion is about the whole home.
 *
 * <p>Invariants:
 * <ul>
 *   <li>REC-INV-001: {@code idRecommendation} and {@code homeId} are required, {@code VARCHAR(10)}</li>
 *   <li>REC-INV-002: {@code deviceId} is optional, {@code VARCHAR(10)}</li>
 *   <li>REC-INV-003: {@code type}, {@code dateTime} and {@code status} are required</li>
 *   <li>REC-INV-004: {@code messageKey} is required, {@code VARCHAR(100)}</li>
 *   <li>REC-INV-005: recommendations are created UNREAD</li>
 * </ul>
 */
public class Recommendation {

    private final String idRecommendation;
    private final String homeId;
    private final String deviceId;
    private final RecommendationType type;
    private final String messageKey;
    private final Instant dateTime;
    private RecommendationStatus status;

    public Recommendation(String idRecommendation, String homeId, String deviceId,
                          RecommendationType type, String messageKey, Instant dateTime,
                          RecommendationStatus status) {
        this.idRecommendation = text(idRecommendation, 10, "idRecommendation");
        this.homeId = text(homeId, 10, "homeId");
        if (deviceId != null && (deviceId.isBlank() || deviceId.length() > 10)) {
            throw new IllegalArgumentException("deviceId must be 1..10 characters");
        }
        this.deviceId = deviceId;
        this.type = Objects.requireNonNull(type, "type");
        this.messageKey = text(messageKey, 100, "messageKey");
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.status = Objects.requireNonNull(status, "status");
    }

    /** @return a new UNREAD recommendation */
    public static Recommendation create(String idRecommendation, String homeId, String deviceId,
                                        RecommendationType type, String messageKey, Instant dateTime) {
        return new Recommendation(idRecommendation, homeId, deviceId, type, messageKey, dateTime,
                RecommendationStatus.UNREAD);
    }

    private static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(field + " must be 1.." + max + " characters");
        }
        return value;
    }

    public String idRecommendation() {
        return idRecommendation;
    }

    public String homeId() {
        return homeId;
    }

    public String deviceId() {
        return deviceId;
    }

    public RecommendationType type() {
        return type;
    }

    public String messageKey() {
        return messageKey;
    }

    public Instant dateTime() {
        return dateTime;
    }

    public RecommendationStatus status() {
        return status;
    }

    public boolean isRead() {
        return status == RecommendationStatus.READ;
    }

    /** Marks it as read. Idempotent: reading it twice is not an error. */
    public void markRead() {
        this.status = RecommendationStatus.READ;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof Recommendation that && idRecommendation.equals(that.idRecommendation);
    }

    @Override
    public int hashCode() {
        return idRecommendation.hashCode();
    }

    @Override
    public String toString() {
        return "Recommendation{id='" + idRecommendation + "', homeId='" + homeId + "', type=" + type
                + ", status=" + status + "}";
    }
}
