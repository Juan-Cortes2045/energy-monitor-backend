package com.energymonitor.measurement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.measurement.application.command.GetLatestMeasurementQuery;
import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.usecase.GetLatestMeasurementService;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetLatestMeasurementServiceTest {

    private MeasurementUseCaseFixtures.FakeMeasurementPersistencePort measurementPort;
    private GetLatestMeasurementService service;

    @BeforeEach
    void setUp() {
        measurementPort = new MeasurementUseCaseFixtures.FakeMeasurementPersistencePort();
        service = new GetLatestMeasurementService(measurementPort);
    }

    @Test
    void getReturnsTheMostRecentReading() {
        measurementPort.seed(Measurement.record("mea0000001", "dev0000001",
                Instant.parse("2026-01-15T08:00:00Z"), 120, 2.5, 100, 1000));
        measurementPort.seed(Measurement.record("mea0000002", "dev0000001",
                Instant.parse("2026-01-15T09:00:00Z"), 120, 2.5, 200, 2000));

        var result = service.get(new GetLatestMeasurementQuery("dev0000001"));

        assertEquals("mea0000002", result.idMeasurement());
        assertEquals(200, result.activePower());
    }

    @Test
    void getWithNoMeasurementsThrows() {
        assertThrows(MeasurementNotFoundException.class,
                () -> service.get(new GetLatestMeasurementQuery("dev0000001")));
    }
}
