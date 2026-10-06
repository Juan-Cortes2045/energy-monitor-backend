package com.energymonitor.home.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.domain.model.HomeType;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code home_type} catalog.
 *
 * <p>This test does NOT depend on the seed {@code home-005} because it is not yet included
 * in {@code home-changelog.xml}. Instead, it inserts its own data within the transaction.
 */
@SpringBootTest
@Transactional
class HomeTypePersistenceTest extends JwtKeyedTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HomeTypePersistenceAdapter homeTypes;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void insertAndFindById() {
        // Insert directly via EntityManager (HomeType is read-only in the catalog)
        entityManager.createNativeQuery(
                "INSERT INTO home_type (id_home_type, name, created_at, updated_at) VALUES (:id, :name, NOW(), NOW())")
                .setParameter("id", "hous000001")
                .setParameter("name", "house")
                .executeUpdate();
        flushAndClear();

        HomeType read = homeTypes.findActive("hous000001").orElseThrow();
        assertEquals("hous000001", read.idHomeType());
        assertEquals("house", read.name());
    }

    @Test
    void findAllActive() {
        entityManager.createNativeQuery(
                "INSERT INTO home_type (id_home_type, name, created_at, updated_at) VALUES (:id, :name, NOW(), NOW())")
                .setParameter("id", "hous000001")
                .setParameter("name", "house")
                .executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO home_type (id_home_type, name, created_at, updated_at) VALUES (:id, :name, NOW(), NOW())")
                .setParameter("id", "apar000001")
                .setParameter("name", "apartment")
                .executeUpdate();
        flushAndClear();

        List<HomeType> types = homeTypes.findAllActive();
        assertTrue(types.size() >= 2);
    }

    @Test
    void findActiveExcludesSoftDeleted() {
        entityManager.createNativeQuery(
                "INSERT INTO home_type (id_home_type, name, created_at, updated_at) VALUES (:id, :name, NOW(), NOW())")
                .setParameter("id", "hous000001")
                .setParameter("name", "house")
                .executeUpdate();
        flushAndClear();

        // Soft delete
        entityManager.createNativeQuery("UPDATE home_type SET deleted_at = NOW() WHERE id_home_type = :id")
                .setParameter("id", "hous000001")
                .executeUpdate();
        flushAndClear();

        assertTrue(homeTypes.findActive("hous000001").isEmpty());
    }
}
