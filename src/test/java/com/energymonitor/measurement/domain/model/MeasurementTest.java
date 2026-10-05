package com.energymonitor.measurement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MeasurementTest {

    private static final Instant DATE_TIME = Instant.parse("2026-01-15T10:30:00Z");

    private Measurement createMeasurement() {
        return Measurement.record("mea0000001", "dev0000001", DATE_TIME, 120.5, 2.5, 300.0, 1520.75);
    }

    @Test
    void recordMeasurementWithValidData() {
        Measurement measurement = createMeasurement();
        assertEquals("mea0000001", measurement.idMeasurement());
        assertEquals("dev0000001", measurement.deviceId());
        assertEquals(DATE_TIME, measurement.dateTime());
        assertEquals(120.5, measurement.getVoltage());
        assertEquals(2.5, measurement.getCurrent());
        assertEquals(300.0, measurement.getActivePower());
        assertEquals(1520.75, measurement.getStoredEnergy());
    }

    @Test
    void recordMeasurementWithZeroValues() {
        Measurement measurement = Measurement.record("mea0000001", "dev0000001", DATE_TIME, 0, 0, 0, 0);
        assertEquals(0, measurement.getVoltage());
        assertEquals(0, measurement.getStoredEnergy());
    }

    @Test
    void recordMeasurementWithBlankIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("", "dev0000001", DATE_TIME, 120.5, 2.5, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithTooLongIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea00000012345", "dev0000001", DATE_TIME, 120.5, 2.5, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithBlankDeviceIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "", DATE_TIME, 120.5, 2.5, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithTooLongDeviceIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "dev00000012345", DATE_TIME, 120.5, 2.5, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithNullDateTimeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "dev0000001", null, 120.5, 2.5, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithNegativeVoltageThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "dev0000001", DATE_TIME, -1, 2.5, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithNegativeCurrentThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "dev0000001", DATE_TIME, 120.5, -0.1, 300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithNegativeActivePowerThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "dev0000001", DATE_TIME, 120.5, 2.5, -300.0, 1520.75));
    }

    @Test
    void recordMeasurementWithNegativeStoredEnergyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Measurement.record("mea0000001", "dev0000001", DATE_TIME, 120.5, 2.5, 300.0, -1520.75));
    }

    @Test
    void equalityIsBasedOnId() {
        Measurement first = createMeasurement();
        Measurement second = Measurement.record("mea0000001", "dev0000002",
                DATE_TIME.plusSeconds(60), 1, 1, 1, 1);
        Measurement different = Measurement.record("mea0000002", "dev0000001", DATE_TIME, 1, 1, 1, 1);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }
}
