package com.energymonitor.measurement.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.api.RiskConsumption;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code consumption_level} catalog.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsumptionLevelPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ConsumptionLevelPersistenceAdapter levels;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByName() {
        MeasurementPersistenceFixtures.seedConsumptionLevel(entityManager,
                "low0000001", RiskConsumption.LOW, 0, 500);
        flushAndClear();

        var read = levels.findActiveByName(RiskConsumption.LOW).orElseThrow();
        assertEquals("low0000001", read.idConsumptionLevel());
        assertEquals(RiskConsumption.LOW, read.name());
        assertEquals(0, read.minLimit());
        assertEquals(500, read.maxLimit());
    }

    @Test
    void listActiveReturnsTheCatalog() {
        MeasurementPersistenceFixtures.seedConsumptionLevel(entityManager,
                "low0000001", RiskConsumption.LOW, 0, 500);
        MeasurementPersistenceFixtures.seedConsumptionLevel(entityManager,
                "medi000001", RiskConsumption.MEDIUM, 500, 1500);
        flushAndClear();

        assertEquals(2, levels.listActive().size());
    }

    @Test
    void findLevelForClassifiesByRange() {
        MeasurementPersistenceFixtures.seedConsumptionLevel(entityManager,
                "low0000001", RiskConsumption.LOW, 0, 500);
        MeasurementPersistenceFixtures.seedConsumptionLevel(entityManager,
                "medi000001", RiskConsumption.MEDIUM, 500, 1500);
        flushAndClear();

        assertEquals(RiskConsumption.LOW, levels.findLevelFor(250).orElseThrow().name());
        // The boundary belongs to the next level: [min, max)
        assertEquals(RiskConsumption.MEDIUM, levels.findLevelFor(500).orElseThrow().name());
        assertTrue(levels.findLevelFor(999999).isEmpty());
    }
}
