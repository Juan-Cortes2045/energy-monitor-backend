package com.energymonitor.measurement.application.port.in;

import com.energymonitor.measurement.application.command.RegisterMeasurementCommand;
import com.energymonitor.measurement.application.result.MeasurementResult;

/**
 * Input port for recording a measurement produced by a device.
 */
public interface RegisterMeasurement {

    /**
     * @param command the reading data
     * @return the recorded measurement
     */
    MeasurementResult register(RegisterMeasurementCommand command);
}
