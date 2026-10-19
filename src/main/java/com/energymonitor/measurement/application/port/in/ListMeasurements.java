package com.energymonitor.measurement.application.port.in;

import com.energymonitor.measurement.application.command.ListMeasurementsQuery;
import com.energymonitor.measurement.application.result.MeasurementResult;
import java.util.List;

/**
 * Input port for reading the consumption history of a device.
 */
public interface ListMeasurements {

    /**
     * @param query the device and optional time range
     * @return the measurements, ordered by {@code dateTime} ascending
     */
    List<MeasurementResult> list(ListMeasurementsQuery query);
}
