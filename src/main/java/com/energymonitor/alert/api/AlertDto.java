package com.energymonitor.alert.api;

import java.time.Instant;

/**
 * Read view of an alert for other modules.
 *
 * @param idAlert            identifier
 * @param homeId             identifier of the home the alert belongs to
 * @param deviceId           identifier of the involved device, {@code null} when home-scoped
 * @param type               what triggered the alert
 * @param messageKey         i18n message key
 * @param dateTime           business timestamp of the event
 * @param alertStatus        lifecycle state
 * @param consumptionLevelId referenced consumption level, populated for THRESHOLD alerts
 * @param measurementId      referenced measurement, populated for THRESHOLD alerts
 */
public record AlertDto(String idAlert, String homeId, String deviceId, AlertType type,
                       String messageKey, Instant dateTime, AlertStatus alertStatus,
                       String consumptionLevelId, String measurementId) {
}
