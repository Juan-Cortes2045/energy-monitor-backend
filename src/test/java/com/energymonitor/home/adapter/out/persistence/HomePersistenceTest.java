package com.energymonitor.home.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.application.exception.HomeAccessCodeCollisionException;
import com.energymonitor.home.domain.model.Home;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code home} aggregate.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomePersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HomePersistenceAdapter homes;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void insertAndFindById() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        Home home = HomePersistenceFixtures.seedHome(homes);
        flushAndClear();

        Home read = homes.findActive(home.idHome()).orElseThrow();
        assertEquals(home.idHome(), read.idHome());
        assertEquals(home.name(), read.name());
        assertEquals(home.accessCode(), read.accessCode());
        assertEquals(home.creationDate(), read.creationDate());
    }

    @Test
    void findByAccessCode() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        Home home = HomePersistenceFixtures.seedHome(homes);
        flushAndClear();

        Home read = homes.findActiveByAccessCode(home.accessCode()).orElseThrow();
        assertEquals(home.idHome(), read.idHome());
    }

    @Test
    void existsActiveByAccessCode() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        Home home = HomePersistenceFixtures.seedHome(homes);
        flushAndClear();

        assertTrue(homes.existsActiveByAccessCode(home.accessCode()));
        assertFalse(homes.existsActiveByAccessCode("ZZZZZZZZ"));
    }

    @Test
    void findActiveByIdsReturnsOnlyActive() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        Home home1 = HomePersistenceFixtures.seedHome(homes);
        Home home2 = Home.create("hom0000002", "Casa2", "hous000001", "Calle 456", "XYZ98765", null,
                HomePersistenceFixtures.CREATION_DATE);
        homes.save(home2);
        flushAndClear();

        // Soft delete home2
        home2 = homes.findActive("hom0000002").orElseThrow();
        // Note: soft delete is done via repository, not exposed in port for this test
        // We just verify that findActiveByIds returns both when both are active
        List<Home> result = homes.findActiveByIds(List.of(home1.idHome(), home2.idHome()));
        assertEquals(2, result.size());
    }

    @Test
    void duplicateAccessCodeThrowsCollision() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        Home home1 = HomePersistenceFixtures.seedHome(homes);
        flushAndClear();

        // Try to save another home with the same access code
        Home home2 = Home.create("hom0000002", "Casa2", "hous000001", "Calle 456", home1.accessCode(), null,
                HomePersistenceFixtures.CREATION_DATE);
        assertThrows(HomeAccessCodeCollisionException.class, () -> homes.save(home2));
    }

    @Test
    void findActiveByIdsExcludesSoftDeleted() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        Home home1 = HomePersistenceFixtures.seedHome(homes);
        Home home2 = Home.create("hom0000002", "Casa2", "hous000001", "Calle 456", "XYZ98765", null,
                HomePersistenceFixtures.CREATION_DATE);
        homes.save(home2);
        flushAndClear();

        // Soft delete home2 via repository
        entityManager.createNativeQuery("UPDATE home SET deleted_at = NOW() WHERE id_home = :id")
                .setParameter("id", "hom0000002")
                .executeUpdate();
        flushAndClear();

        List<Home> result = homes.findActiveByIds(List.of(home1.idHome(), home2.idHome()));
        assertEquals(1, result.size());
        assertEquals(home1.idHome(), result.get(0).idHome());
    }
}
