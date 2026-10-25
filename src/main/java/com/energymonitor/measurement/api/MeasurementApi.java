package com.energymonitor.measurement.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Readings as the other modules of this backend read them. They live in the measurement service,
 * which answers through its internal API; see {@code RemoteMeasurementApi}.
 */
public interface MeasurementApi {

    /**
     * Classifies an active power value into its consumption level.
     *
     * @param activePower the value to classify
     * @return the level whose {@code [minLimit, maxLimit)} range holds it, empty when none does
     * @throws MeasurementServiceUnavailableException when the levels cannot be read and none were
     *                                                kept from an earlier answer
     */
    Optional<ConsumptionLevelDto> classify(double activePower);

    /**
     * Energy per device and UTC hour, computed from the cumulative counter the same way as the
     * consumption of a home.
     *
     * @return one entry per device and hour with readings, oldest first
     * @throws MeasurementServiceUnavailableException when the service does not answer
     */
    List<HourlyEnergyDto> hourlyEnergy(Collection<String> deviceIds, Instant from, Instant to);

    /**
     * Devices that published at least one reading since an instant.
     *
     * @throws MeasurementServiceUnavailableException when the service does not answer
     */
    List<String> devicesReportingSince(Instant since);
}
