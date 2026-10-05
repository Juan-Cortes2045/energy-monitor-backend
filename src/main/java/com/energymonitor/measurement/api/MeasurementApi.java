package com.energymonitor.measurement.api;

import java.util.Optional;

/**
 * Public API of the Monitoring and Measurements bounded context.
 *
 * <p>Other modules (e.g. {@code alert}, {@code recommendation}) use this interface to read
 * measurement data and consumption levels without depending on the module's internals.
 */
public interface MeasurementApi {

    /**
     * Finds the most recent measurement of a device.
     *
     * @param deviceId the device identifier
     * @return the latest measurement DTO, empty when the device has none
     */
    Optional<MeasurementDto> findLatestByDevice(String deviceId);

    /**
     * Classifies an active power value into its consumption level.
     *
     * @param activePower the value to classify
     * @return the matching level DTO, empty when no level covers the value
     */
    Optional<ConsumptionLevelDto> classify(double activePower);

    /**
     * Finds a consumption level by name.
     *
     * @param name the risk classification
     * @return the level DTO, empty when not found
     */
    Optional<ConsumptionLevelDto> findLevel(RiskConsumption name);
}
