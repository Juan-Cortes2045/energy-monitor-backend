package com.energymonitor.measurement.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.adapter.out.persistence.entity.ConsumptionLevelEntity;
import com.energymonitor.measurement.api.RiskConsumption;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code consumption_level} catalog.
 *
 * <p>The catalog is not seeded by the test: the {@code measurement-003-seed-consumption-level}
 * changeset already loads the four levels during migration, so these tests assert against
 * that data (LOW 0–500, MEDIUM 500–1500, HIGH 1500–3000, CRITICAL 3000–1000000).
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
        flushAndClear();

        var read = levels.findActiveByName(RiskConsumption.LOW).orElseThrow();
        assertEquals("low0000001", read.idConsumptionLevel());
        assertEquals(RiskConsumption.LOW, read.name());
        assertEquals(0, read.minLimit());
        assertEquals(500, read.maxLimit());
    }

    @Test
    void listActiveReturnsTheSeededCatalog() {
        flushAndClear();

        assertEquals(4, levels.listActive().size());
    }

    @Test
    void findLevelForClassifiesByRange() {
        flushAndClear();

        assertEquals(RiskConsumption.LOW, levels.findLevelFor(250).orElseThrow().name());
        // The boundary belongs to the next level: [min, max)
        assertEquals(RiskConsumption.MEDIUM, levels.findLevelFor(500).orElseThrow().name());
        assertEquals(RiskConsumption.CRITICAL, levels.findLevelFor(5000).orElseThrow().name());
        // Beyond the CRITICAL sentinel upper bound no level applies
        assertTrue(levels.findLevelFor(1_500_000).isEmpty());
    }

    @Test
    void softDeletedLevelsAreExcluded() {
        entityManager.find(ConsumptionLevelEntity.class, "low0000001")
                .setDeletedAt(MeasurementPersistenceFixtures.T1);
        flushAndClear();

        assertTrue(levels.findActiveByName(RiskConsumption.LOW).isEmpty());
        assertEquals(3, levels.listActive().size());
    }
}
