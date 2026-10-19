package com.energymonitor.measurement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.application.command.ListMeasurementsQuery;
import com.energymonitor.measurement.application.usecase.ListMeasurementsService;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListMeasurementsServiceTest {

    private static final Instant T1 = Instant.parse("2026-01-15T08:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-15T09:00:00Z");
    private static final Instant T3 = Instant.parse("2026-01-15T10:00:00Z");

    private MeasurementUseCaseFixtures.FakeMeasurementPersistencePort measurementPort;
    private ListMeasurementsService service;

    @BeforeEach
    void setUp() {
        measurementPort = new MeasurementUseCaseFixtures.FakeMeasurementPersistencePort();
        service = new ListMeasurementsService(measurementPort, MeasurementUseCaseFixtures.clock());
    }

    private void seedThree() {
        measurementPort.seed(Measurement.record("mea0000003", "dev0000001", T3, 120, 2.5, 300, 3000));
        measurementPort.seed(Measurement.record("mea0000001", "dev0000001", T1, 120, 2.5, 100, 1000));
        measurementPort.seed(Measurement.record("mea0000002", "dev0000001", T2, 120, 2.5, 200, 2000));
        // Another device, must not leak into the results
        measurementPort.seed(Measurement.record("mea0000004", "dev0000002", T2, 120, 2.5, 999, 2000));
    }

    @Test
    void listReturnsDeviceHistoryOrderedAscending() {
        seedThree();
        var results = service.list(new ListMeasurementsQuery("dev0000001", null, null));

        assertEquals(3, results.size());
        assertEquals("mea0000001", results.get(0).idMeasurement());
        assertEquals("mea0000002", results.get(1).idMeasurement());
        assertEquals("mea0000003", results.get(2).idMeasurement());
    }

    @Test
    void listWithNoMeasurementsReturnsEmpty() {
        var results = service.list(new ListMeasurementsQuery("dev0000001", null, null));
        assertTrue(results.isEmpty());
    }

    @Test
    void listHonorsTheLowerBound() {
        seedThree();
        var results = service.list(new ListMeasurementsQuery("dev0000001", T2, null));

        assertEquals(2, results.size());
        assertEquals("mea0000002", results.get(0).idMeasurement());
    }

    @Test
    void listHonorsTheUpperBound() {
        seedThree();
        var results = service.list(new ListMeasurementsQuery("dev0000001", null, T2));

        assertEquals(2, results.size());
        assertEquals("mea0000002", results.get(1).idMeasurement());
    }
}
