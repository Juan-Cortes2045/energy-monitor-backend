package com.energymonitor.measurement.application.port.in;

import com.energymonitor.measurement.application.command.GetLatestMeasurementQuery;
import com.energymonitor.measurement.application.result.MeasurementResult;

/**
 * Input port for reading the most recent measurement of a device.
 */
public interface GetLatestMeasurement {

    /**
     * @param query the device
     * @return the latest measurement
     * @throws com.energymonitor.measurement.application.exception.MeasurementNotFoundException
     *         when the device has no measurements
     */
    MeasurementResult get(GetLatestMeasurementQuery query);
}
