package com.energymonitor.home.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.domain.model.HomeThresholds;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code home_thresholds} aggregate.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomeThresholdsPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HomePersistenceAdapter homes;

    @Autowired
    private HomeThresholdsPersistenceAdapter thresholds;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void insertAndFindByHomeId() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomeThresholds t = HomePersistenceFixtures.seedThresholds(thresholds);
        flushAndClear();

        HomeThresholds read = thresholds.findActiveByHomeId(t.homeId()).orElseThrow();
        assertEquals(t.idThreshold(), read.idThreshold());
        assertEquals(t.dailyLimit(), read.dailyLimit());
        assertEquals(t.monthlyLimit(), read.monthlyLimit());
        assertTrue(read.isUseSystemDefault());
    }

    @Test
    void updateThresholds() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomeThresholds t = HomePersistenceFixtures.seedThresholds(thresholds);
        flushAndClear();

        t.update(15.0, 450.0);
        thresholds.save(t);
        flushAndClear();

        HomeThresholds read = thresholds.findActiveByHomeId(t.homeId()).orElseThrow();
        assertEquals(15.0, read.dailyLimit());
        assertEquals(450.0, read.monthlyLimit());
        assertFalse(read.isUseSystemDefault());
    }

    @Test
    void findActiveByHomeIdExcludesSoftDeleted() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomeThresholds t = HomePersistenceFixtures.seedThresholds(thresholds);
        flushAndClear();

        // Soft delete
        entityManager.createNativeQuery("UPDATE home_thresholds SET deleted_at = NOW() WHERE id_threshold = :id")
                .setParameter("id", t.idThreshold())
                .executeUpdate();
        flushAndClear();

        assertTrue(thresholds.findActiveByHomeId(t.homeId()).isEmpty());
    }
}
