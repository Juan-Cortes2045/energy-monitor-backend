package com.energymonitor.alert.api;

import java.util.List;
import java.util.Optional;

/**
 * Public API of the Alerts bounded context.
 *
 * <p>Other modules (e.g. {@code notification}) use this interface to read alert data
 * without depending on the module's internals.
 */
public interface AlertApi {

    /**
     * Finds an alert by identifier.
     *
     * @param idAlert the alert identifier
     * @return the alert DTO, empty when not found
     */
    Optional<AlertDto> findAlert(String idAlert);

    /**
     * Lists the pending alerts of a home.
     *
     * @param homeId the home identifier
     * @return the pending alerts, most recent first
     */
    List<AlertDto> listPendingByHome(String homeId);
}
