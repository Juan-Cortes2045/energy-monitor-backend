package com.energymonitor.measurement.application.result;

import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;

/**
 * Result of a measurement query.
 *
 * @param idMeasurement identifier
 * @param deviceId      identifier of the measuring device
 * @param dateTime      business timestamp of the reading
 * @param voltage       voltage reading
 * @param current       current reading
 * @param activePower   active power reading
 * @param storedEnergy  accumulated energy reading
 */
public record MeasurementResult(String idMeasurement, String deviceId, Instant dateTime,
                                double voltage, double current, double activePower, double storedEnergy) {

    /**
     * Creates a result from a domain object.
     *
     * @param measurement the domain object
     * @return the result
     */
    public static MeasurementResult from(Measurement measurement) {
        return new MeasurementResult(measurement.idMeasurement(), measurement.deviceId(),
                measurement.dateTime(), measurement.getVoltage(), measurement.getCurrent(),
                measurement.getActivePower(), measurement.getStoredEnergy());
    }
}
