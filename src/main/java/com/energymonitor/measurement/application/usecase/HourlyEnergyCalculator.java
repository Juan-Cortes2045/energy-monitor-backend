package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.application.port.out.HourlyEnergyRow;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Energy consumed per device and UTC hour, from the cumulative {@code storedEnergy} counter.
 *
 * <p>The energy of an hour is its highest reading minus the highest reading of the previous hour
 * (or the last reading before the range, for the first one). A counter that goes backwards (meter
 * reset or replaced) restarts the sequence: that hour counts only its own spread. Energy is never
 * negative.
 *
 * <p>Shared by the consumption use case and by {@code measurement::api}, so every consumer of
 * energy figures computes them the same way. No Spring annotations.
 */
public class HourlyEnergyCalculator {

    /**
     * @param averagePower mean active power in the hour, W
     * @param energy       energy consumed in the hour, kWh
     */
    public record HourEnergy(Instant start, double averagePower, double energy) {
    }

    private final MeasurementPersistencePort measurements;

    public HourlyEnergyCalculator(MeasurementPersistencePort measurements) {
        this.measurements = measurements;
    }

    /** Energy of every hour with readings, per device, oldest first. */
    public Map<String, List<HourEnergy>> compute(Collection<String> deviceIds, Instant from, Instant to) {
        Map<String, List<HourEnergy>> result = new LinkedHashMap<>();
        if (deviceIds.isEmpty()) {
            return result;
        }
        Map<String, Double> previous = new LinkedHashMap<>();
        for (String deviceId : deviceIds) {
            measurements.findLatestActiveByDeviceBefore(deviceId, from)
                    .ifPresent(m -> previous.put(deviceId, m.getStoredEnergy()));
        }
        for (HourlyEnergyRow row : measurements.aggregateHourly(deviceIds, from, to)) {
            Double before = previous.get(row.deviceId());
            double energy = before == null || row.maxStoredEnergy() < before
                    ? row.maxStoredEnergy() - row.minStoredEnergy()
                    : row.maxStoredEnergy() - before;
            previous.put(row.deviceId(), row.maxStoredEnergy());
            result.computeIfAbsent(row.deviceId(), id -> new ArrayList<>())
                    .add(new HourEnergy(row.hourStart(), row.averagePower(), Math.max(0, energy)));
        }
        return result;
    }
}
