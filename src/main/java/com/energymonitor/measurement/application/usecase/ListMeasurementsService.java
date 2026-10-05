package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.application.command.ListMeasurementsQuery;
import com.energymonitor.measurement.application.port.in.ListMeasurements;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.application.result.MeasurementResult;
import java.time.Clock;
import java.util.List;

/**
 * Reads the consumption history of a device, optionally bounded to a time range.
 *
 * <p>Read-only: no transaction boundary, mirroring the single-query use cases of the
 * other modules. This service contains no Spring annotations.
 */
public class ListMeasurementsService implements ListMeasurements {

    private final MeasurementPersistencePort measurementPort;
    private final Clock clock;

    public ListMeasurementsService(MeasurementPersistencePort measurementPort, Clock clock) {
        this.measurementPort = measurementPort;
        this.clock = clock;
    }

    @Override
    public List<MeasurementResult> list(ListMeasurementsQuery query) {
        TimeRange range = TimeRange.of(query.from(), query.to(), clock);
        return measurementPort.listActiveByDevice(query.deviceId(), range.from(), range.to())
                .stream()
                .map(MeasurementResult::from)
                .toList();
    }
}
