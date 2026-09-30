package com.energymonitor.home.application.port.out;

import com.energymonitor.home.domain.model.HomeThresholds;
import java.util.Optional;

/**
 * Output port for persisting and querying home thresholds.
 */
public interface HomeThresholdsPersistencePort {

    /**
     * Saves thresholds (insert or update).
     *
     * @param thresholds the domain object
     * @return the same domain object
     */
    HomeThresholds save(HomeThresholds thresholds);

    /**
     * Finds active thresholds by home identifier.
     *
     * @param homeId the home identifier
     * @return the thresholds, empty when soft-deleted or missing
     */
    Optional<HomeThresholds> findActiveByHomeId(String homeId);
}
