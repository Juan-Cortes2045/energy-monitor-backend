package com.energymonitor.measurement.application.port.out;

import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting and querying measurements.
 */
public interface MeasurementPersistencePort {

    /**
     * Saves a measurement (insert or update).
     *
     * @param measurement the domain object
     * @return the same domain object
     */
    Measurement save(Measurement measurement);

    /**
     * Finds an active measurement by identifier.
     *
     * @param idMeasurement the identifier
     * @return the measurement, empty when soft-deleted or missing
     */
    Optional<Measurement> findActive(String idMeasurement);

    /**
     * Lists the active measurements of a device inside a time range.
     *
     * @param deviceId the device identifier
     * @param from     inclusive lower bound of {@code dateTime}
     * @param to       inclusive upper bound of {@code dateTime}
     * @return the measurements, ordered by {@code dateTime} ascending
     */
    List<Measurement> listActiveByDevice(String deviceId, Instant from, Instant to);

    /**
     * Finds the most recent active measurement of a device.
     *
     * @param deviceId the device identifier
     * @return the latest measurement, empty when the device has none
     */
    Optional<Measurement> findLatestActiveByDevice(String deviceId);
}
