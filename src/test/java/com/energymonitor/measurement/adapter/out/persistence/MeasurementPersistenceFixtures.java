package com.energymonitor.measurement.adapter.out.persistence;

import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;

/**
 * Shared seeds for persistence tests.
 */
final class MeasurementPersistenceFixtures {

    static final String DEVICE_ID = "tstmeas001";
    static final Instant T1 = Instant.parse("2026-01-15T08:00:00Z");
    static final Instant T2 = Instant.parse("2026-01-15T09:00:00Z");
    static final Instant T3 = Instant.parse("2026-01-15T10:00:00Z");

    private MeasurementPersistenceFixtures() {
    }

    static Measurement seedMeasurement(MeasurementPersistenceAdapter measurements,
                                       String id, Instant dateTime, double activePower, double storedEnergy) {
        Measurement measurement = Measurement.record(id, DEVICE_ID, dateTime, 120.5, 2.5,
                activePower, storedEnergy);
        measurements.save(measurement);
        return measurement;
    }
}
