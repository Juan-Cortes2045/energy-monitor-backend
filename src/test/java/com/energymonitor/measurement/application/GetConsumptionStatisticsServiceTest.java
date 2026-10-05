package com.energymonitor.measurement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.measurement.application.command.GetConsumptionStatisticsQuery;
import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.usecase.GetConsumptionStatisticsService;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetConsumptionStatisticsServiceTest {

    private static final Instant T1 = Instant.parse("2026-01-15T08:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-15T09:00:00Z");
    private static final Instant T3 = Instant.parse("2026-01-15T10:00:00Z");

    private MeasurementUseCaseFixtures.FakeMeasurementPersistencePort measurementPort;
    private GetConsumptionStatisticsService service;

    @BeforeEach
    void setUp() {
        measurementPort = new MeasurementUseCaseFixtures.FakeMeasurementPersistencePort();
        service = new GetConsumptionStatisticsService(measurementPort, MeasurementUseCaseFixtures.clock());
    }

    private void seedThree() {
        measurementPort.seed(Measurement.record("mea0000001", "dev0000001", T1, 120, 2.5, 100, 1000));
        measurementPort.seed(Measurement.record("mea0000002", "dev0000001", T2, 120, 2.5, 300, 1300));
        measurementPort.seed(Measurement.record("mea0000003", "dev0000001", T3, 120, 2.5, 200, 1750));
    }

    @Test
    void aggregatesTheRange() {
        seedThree();
        var result = service.get(new GetConsumptionStatisticsQuery("dev0000001", T1, T3));

        assertEquals("dev0000001", result.deviceId());
        assertEquals(T1, result.from());
        assertEquals(T3, result.to());
        assertEquals(3, result.measurementCount());
        assertEquals(200.0, result.averageActivePower());
        assertEquals(300.0, result.maxActivePower());
        // storedEnergy delta: 1750 - 1000
        assertEquals(750.0, result.consumedEnergy());
    }

    @Test
    void consumedEnergyIsZeroForASingleReading() {
        measurementPort.seed(Measurement.record("mea0000001", "dev0000001", T1, 120, 2.5, 100, 1000));
        var result = service.get(new GetConsumptionStatisticsQuery("dev0000001", null, null));

        assertEquals(1, result.measurementCount());
        assertEquals(0, result.consumedEnergy());
    }

    @Test
    void counterResetIsClampedToZero() {
        // The device counter restarts: the delta would be negative without clamping
        measurementPort.seed(Measurement.record("mea0000001", "dev0000001", T1, 120, 2.5, 100, 5000));
        measurementPort.seed(Measurement.record("mea0000002", "dev0000001", T2, 120, 2.5, 100, 50));

        var result = service.get(new GetConsumptionStatisticsQuery("dev0000001", null, null));

        assertEquals(0, result.consumedEnergy());
    }

    @Test
    void defaultRangeUsesTheServerClockAsUpperBound() {
        seedThree();
        var result = service.get(new GetConsumptionStatisticsQuery("dev0000001", null, null));

        assertEquals(MeasurementUseCaseFixtures.NOW, result.to());
        assertEquals(3, result.measurementCount());
    }

    @Test
    void emptyRangeThrows() {
        assertThrows(MeasurementNotFoundException.class,
                () -> service.get(new GetConsumptionStatisticsQuery("dev0000001", T1, T3)));
    }
}
