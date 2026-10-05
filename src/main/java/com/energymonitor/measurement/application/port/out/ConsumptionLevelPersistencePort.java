package com.energymonitor.measurement.application.port.out;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import java.util.List;
import java.util.Optional;

/**
 * Output port for querying the consumption level catalog.
 */
public interface ConsumptionLevelPersistencePort {

    /**
     * Finds an active level by name.
     *
     * @param name the risk classification
     * @return the level, empty when soft-deleted or missing
     */
    Optional<ConsumptionLevel> findActiveByName(RiskConsumption name);

    /**
     * Lists every active level.
     *
     * @return the catalog, in no particular order
     */
    List<ConsumptionLevel> listActive();

    /**
     * Finds the active level whose range covers the given value.
     *
     * @param activePower the value to classify
     * @return the matching level, empty when no level covers the value
     */
    Optional<ConsumptionLevel> findLevelFor(double activePower);
}
