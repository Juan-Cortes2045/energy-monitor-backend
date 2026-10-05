package com.energymonitor.alert.api;

import java.time.Instant;

/**
 * Application event published after an alert is persisted.
 *
 * <p>The {@code notification} module is expected to subscribe to this event to deliver
 * the alert to the home's members; the payload already carries everything it needs to
 * build the message without querying this module back.
 *
 * @param idAlert            identifier of the raised alert
 * @param homeId             identifier of the home the alert belongs to
 * @param deviceId           identifier of the involved device, {@code null} when home-scoped
 * @param type               what triggered the alert
 * @param messageKey         i18n message key
 * @param dateTime           business timestamp of the event
 * @param consumptionLevelId referenced consumption level, populated for THRESHOLD alerts
 * @param measurementId      referenced measurement, populated for THRESHOLD alerts
 */
public record AlertRaised(String idAlert, String homeId, String deviceId, AlertType type,
                          String messageKey, Instant dateTime,
                          String consumptionLevelId, String measurementId) {
}
