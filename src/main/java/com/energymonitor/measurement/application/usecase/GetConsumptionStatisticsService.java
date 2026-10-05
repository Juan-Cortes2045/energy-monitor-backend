package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.application.command.GetConsumptionStatisticsQuery;
import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.port.in.GetConsumptionStatistics;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.application.result.ConsumptionStatisticsResult;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Clock;
import java.util.List;

/**
 * Aggregates the readings of a device over a time range.
 *
 * <p><strong>Consumed energy approximation:</strong> {@code storedEnergy} is a cumulative
 * counter per device, so the energy consumed in the range is the delta between the last
 * and the first reading <em>inside</em> it. The true baseline would be the reading right
 * before the range; the approximation is documented and good enough for a first cut.
 * A counter reset (device replacement or rollover) shows up as a negative delta and is
 * clamped to zero.
 *
 * <p>Read-only: no transaction boundary. This service contains no Spring annotations.
 */
public class GetConsumptionStatisticsService implements GetConsumptionStatistics {

    private final MeasurementPersistencePort measurementPort;
    private final Clock clock;

    public GetConsumptionStatisticsService(MeasurementPersistencePort measurementPort, Clock clock) {
        this.measurementPort = measurementPort;
        this.clock = clock;
    }

    @Override
    public ConsumptionStatisticsResult get(GetConsumptionStatisticsQuery query) {
        TimeRange range = TimeRange.of(query.from(), query.to(), clock);
        List<Measurement> measurements =
                measurementPort.listActiveByDevice(query.deviceId(), range.from(), range.to());
        if (measurements.isEmpty()) {
            throw new MeasurementNotFoundException(
                    "no measurements for device " + query.deviceId() + " in the requested range");
        }

        double averageActivePower = measurements.stream()
                .mapToDouble(Measurement::getActivePower)
                .average()
                .orElseThrow();
        double maxActivePower = measurements.stream()
                .mapToDouble(Measurement::getActivePower)
                .max()
                .orElseThrow();
        // The port returns the readings ordered by dateTime ascending
        double consumedEnergy = Math.max(0,
                measurements.getLast().getStoredEnergy() - measurements.getFirst().getStoredEnergy());

        return new ConsumptionStatisticsResult(query.deviceId(), range.from(), range.to(),
                measurements.size(), averageActivePower, maxActivePower, consumedEnergy);
    }
}
