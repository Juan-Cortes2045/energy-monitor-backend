package com.energymonitor.measurement.api;

/**
 * Read view of a consumption level for other modules.
 *
 * @param idConsumptionLevel identifier
 * @param name               risk classification
 * @param description        human-readable description
 * @param minLimit           inclusive lower bound of the range
 * @param maxLimit           exclusive upper bound of the range
 */
public record ConsumptionLevelDto(String idConsumptionLevel, RiskConsumption name, String description,
                                  double minLimit, double maxLimit) {
}
