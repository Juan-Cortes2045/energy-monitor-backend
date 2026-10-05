package com.energymonitor.measurement.adapter.in.web.dto;

import com.energymonitor.measurement.api.RiskConsumption;

/**
 * Response DTO for a consumption level.
 *
 * @param idConsumptionLevel identifier
 * @param name               risk classification
 * @param description        human-readable description
 * @param minLimit           inclusive lower bound of the range
 * @param maxLimit           exclusive upper bound of the range
 */
public record ConsumptionLevelResponse(String idConsumptionLevel, RiskConsumption name,
                                       String description, double minLimit, double maxLimit) {
}
