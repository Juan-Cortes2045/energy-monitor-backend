package com.energymonitor.alert.application.port.out;

import com.energymonitor.measurement.api.RiskConsumption;
import java.util.Optional;

/**
 * Output port for classifying an active power value into a consumption level.
 *
 * <p>Implemented by an adapter that calls the measurement module's public API, so the
 * application layer never reaches across the module boundary itself.
 */
public interface ConsumptionLevelLookupPort {

    /**
     * @param activePower the value to classify
     * @return the matching level view, empty when no level covers the value
     */
    Optional<ConsumptionLevelView> classify(double activePower);

    /**
     * The slice of consumption level data this context needs.
     *
     * @param idConsumptionLevel identifier of the level
     * @param name               risk classification
     */
    record ConsumptionLevelView(String idConsumptionLevel, RiskConsumption name) {
    }
}
