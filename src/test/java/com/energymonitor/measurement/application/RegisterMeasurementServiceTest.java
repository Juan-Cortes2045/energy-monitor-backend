package com.energymonitor.measurement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.api.MeasurementRecorded;
import com.energymonitor.measurement.application.command.RegisterMeasurementCommand;
import com.energymonitor.measurement.application.usecase.RegisterMeasurementService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterMeasurementServiceTest {

    private static final Instant READING_TIME = Instant.parse("2026-01-15T09:00:00Z");

    private MeasurementUseCaseFixtures.FakeMeasurementPersistencePort measurementPort;
    private MeasurementUseCaseFixtures.FakeIdentifierGeneratorPort identifiers;
    private MeasurementUseCaseFixtures.FakeMeasurementEventPort events;
    private RegisterMeasurementService service;

    @BeforeEach
    void setUp() {
        measurementPort = new MeasurementUseCaseFixtures.FakeMeasurementPersistencePort();
        identifiers = new MeasurementUseCaseFixtures.FakeIdentifierGeneratorPort();
        events = new MeasurementUseCaseFixtures.FakeMeasurementEventPort();
        service = new RegisterMeasurementService(measurementPort, identifiers, events,
                MeasurementUseCaseFixtures.clock());
    }

    @Test
    void registerWithExplicitDateTime() {
        var command = new RegisterMeasurementCommand("dev0000001", READING_TIME, 120.5, 2.5, 300.0, 1520.75);
        var result = service.register(command);

        assertNotNull(result);
        assertEquals("id0000001", result.idMeasurement());
        assertEquals("dev0000001", result.deviceId());
        assertEquals(READING_TIME, result.dateTime());
        assertEquals(120.5, result.voltage());
        assertEquals(2.5, result.current());
        assertEquals(300.0, result.activePower());
        assertEquals(1520.75, result.storedEnergy());

        // Verify the measurement was persisted
        assertTrue(measurementPort.findActive(result.idMeasurement()).isPresent());
    }

    @Test
    void registerWithoutDateTimeUsesServerClock() {
        var command = new RegisterMeasurementCommand("dev0000001", null, 120.5, 2.5, 300.0, 1520.75);
        var result = service.register(command);

        assertEquals(MeasurementUseCaseFixtures.NOW, result.dateTime());
    }

    @Test
    void registerPublishesMeasurementRecordedEvent() {
        var command = new RegisterMeasurementCommand("dev0000001", READING_TIME, 120.5, 2.5, 300.0, 1520.75);
        var result = service.register(command);

        assertEquals(1, events.published.size());
        MeasurementRecorded event = events.published.getFirst();
        assertEquals(result.idMeasurement(), event.idMeasurement());
        assertEquals(result.deviceId(), event.deviceId());
        assertEquals(result.dateTime(), event.dateTime());
        assertEquals(result.activePower(), event.activePower());
        assertEquals(result.storedEnergy(), event.storedEnergy());
    }

    @Test
    void registerWithNegativeValueThrowsAndPublishesNothing() {
        var command = new RegisterMeasurementCommand("dev0000001", READING_TIME, 120.5, 2.5, -300.0, 1520.75);
        assertThrows(IllegalArgumentException.class, () -> service.register(command));
        assertTrue(events.published.isEmpty());
    }
}
