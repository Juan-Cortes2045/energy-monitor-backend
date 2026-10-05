package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.application.command.GetLatestMeasurementQuery;
import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.port.in.GetLatestMeasurement;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.application.result.MeasurementResult;

/**
 * Reads the most recent measurement of a device.
 *
 * <p>Read-only: no transaction boundary. This service contains no Spring annotations.
 */
public class GetLatestMeasurementService implements GetLatestMeasurement {

    private final MeasurementPersistencePort measurementPort;

    public GetLatestMeasurementService(MeasurementPersistencePort measurementPort) {
        this.measurementPort = measurementPort;
    }

    @Override
    public MeasurementResult get(GetLatestMeasurementQuery query) {
        return measurementPort.findLatestActiveByDevice(query.deviceId())
                .map(MeasurementResult::from)
                .orElseThrow(() -> new MeasurementNotFoundException(
                        "no measurements for device " + query.deviceId()));
    }
}
