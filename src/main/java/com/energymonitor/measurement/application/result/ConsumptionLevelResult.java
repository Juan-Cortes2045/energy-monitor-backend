package com.energymonitor.measurement.application.result;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;

/**
 * Result of a consumption level query.
 *
 * @param idConsumptionLevel identifier
 * @param name               risk classification
 * @param description        human-readable description
 * @param minLimit           inclusive lower bound of the range
 * @param maxLimit           exclusive upper bound of the range
 */
public record ConsumptionLevelResult(String idConsumptionLevel, RiskConsumption name, String description,
                                     double minLimit, double maxLimit) {

    /**
     * Creates a result from a domain object.
     *
     * @param level the domain object
     * @return the result
     */
    public static ConsumptionLevelResult from(ConsumptionLevel level) {
        return new ConsumptionLevelResult(level.idConsumptionLevel(), level.name(),
                level.description(), level.minLimit(), level.maxLimit());
    }
}
