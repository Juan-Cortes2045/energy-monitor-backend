package com.energymonitor.measurement.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.adapter.out.persistence.entity.MeasurementEntity;
import com.energymonitor.measurement.domain.model.Measurement;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code measurement} aggregate.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeasurementPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MeasurementPersistenceAdapter measurements;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void insertAndFindById() {
        Measurement measurement = MeasurementPersistenceFixtures.seedMeasurement(
                measurements, "mea0000001", MeasurementPersistenceFixtures.T1, 300.0, 1520.0);
        flushAndClear();

        Measurement read = measurements.findActive(measurement.idMeasurement()).orElseThrow();
        assertEquals(measurement.idMeasurement(), read.idMeasurement());
        assertEquals(measurement.deviceId(), read.deviceId());
        assertEquals(measurement.dateTime(), read.dateTime());
        assertEquals(measurement.getVoltage(), read.getVoltage());
        assertEquals(measurement.getCurrent(), read.getCurrent());
        assertEquals(measurement.getActivePower(), read.getActivePower());
        assertEquals(measurement.getStoredEnergy(), read.getStoredEnergy());
    }

    @Test
    void listByDeviceHonorsRangeAndOrder() {
        MeasurementPersistenceFixtures.seedMeasurement(measurements, "mea0000003",
                MeasurementPersistenceFixtures.T3, 300.0, 3000.0);
        MeasurementPersistenceFixtures.seedMeasurement(measurements, "mea0000001",
                MeasurementPersistenceFixtures.T1, 100.0, 1000.0);
        MeasurementPersistenceFixtures.seedMeasurement(measurements, "mea0000002",
                MeasurementPersistenceFixtures.T2, 200.0, 2000.0);
        flushAndClear();

        List<Measurement> all = measurements.listActiveByDevice(
                MeasurementPersistenceFixtures.DEVICE_ID,
                MeasurementPersistenceFixtures.T1, MeasurementPersistenceFixtures.T3);
        assertEquals(3, all.size());
        assertEquals("mea0000001", all.get(0).idMeasurement());
        assertEquals("mea0000002", all.get(1).idMeasurement());
        assertEquals("mea0000003", all.get(2).idMeasurement());

        List<Measurement> bounded = measurements.listActiveByDevice(
                MeasurementPersistenceFixtures.DEVICE_ID,
                MeasurementPersistenceFixtures.T2, MeasurementPersistenceFixtures.T3);
        assertEquals(2, bounded.size());
        assertEquals("mea0000002", bounded.get(0).idMeasurement());
    }

    @Test
    void findLatestReturnsTheMostRecent() {
        MeasurementPersistenceFixtures.seedMeasurement(measurements, "mea0000001",
                MeasurementPersistenceFixtures.T1, 100.0, 1000.0);
        MeasurementPersistenceFixtures.seedMeasurement(measurements, "mea0000002",
                MeasurementPersistenceFixtures.T2, 200.0, 2000.0);
        flushAndClear();

        Measurement latest = measurements
                .findLatestActiveByDevice(MeasurementPersistenceFixtures.DEVICE_ID)
                .orElseThrow();
        assertEquals("mea0000002", latest.idMeasurement());
    }

    @Test
    void softDeletedRowsAreExcluded() {
        Measurement measurement = MeasurementPersistenceFixtures.seedMeasurement(
                measurements, "mea0000001", MeasurementPersistenceFixtures.T1, 300.0, 1520.0);
        flushAndClear();

        entityManager.find(MeasurementEntity.class, measurement.idMeasurement())
                .setDeletedAt(MeasurementPersistenceFixtures.T2);
        flushAndClear();

        assertTrue(measurements.findActive(measurement.idMeasurement()).isEmpty());
        assertTrue(measurements
                .findLatestActiveByDevice(MeasurementPersistenceFixtures.DEVICE_ID).isEmpty());
        assertTrue(measurements.listActiveByDevice(MeasurementPersistenceFixtures.DEVICE_ID,
                MeasurementPersistenceFixtures.T1, MeasurementPersistenceFixtures.T3).isEmpty());
    }
}
