package com.energymonitor.home.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the {@code user_home} membership.
 */
@SpringBootTest
@Transactional
class UserHomePersistenceTest extends JwtKeyedTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HomePersistenceAdapter homes;

    @Autowired
    private UserHomePersistenceAdapter userHomes;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void insertAndFindActive() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        UserHome membership = HomePersistenceFixtures.seedOwner(userHomes);
        flushAndClear();

        UserHome read = userHomes.findActive(membership.userId(), membership.homeId()).orElseThrow();
        assertEquals(membership.userId(), read.userId());
        assertEquals(membership.homeId(), read.homeId());
        assertEquals(Role.OWNER, read.role());
        assertFalse(read.isFavorite());
    }

    @Test
    void removeSoftDeletes() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        UserHome membership = HomePersistenceFixtures.seedOwner(userHomes);
        flushAndClear();

        userHomes.remove(membership.userId(), membership.homeId());
        flushAndClear();

        assertTrue(userHomes.findActive(membership.userId(), membership.homeId()).isEmpty());
    }

    @Test
    void reactivationAfterLeave() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        UserHome membership = HomePersistenceFixtures.seedMember(userHomes, "use0000002");
        flushAndClear();

        // Leave
        userHomes.remove("use0000002", membership.homeId());
        flushAndClear();
        assertTrue(userHomes.findActive("use0000002", membership.homeId()).isEmpty());

        // Re-join: should reactivate as MEMBER with favorite = false
        UserHome newMembership = UserHome.member("use0000002", membership.homeId());
        userHomes.save(newMembership);
        flushAndClear();

        UserHome reactivated = userHomes.findActive("use0000002", membership.homeId()).orElseThrow();
        assertEquals(Role.MEMBER, reactivated.role());
        assertFalse(reactivated.isFavorite());
    }

    @Test
    void countActiveOwnersByHomeId() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomePersistenceFixtures.seedOwner(userHomes);
        HomePersistenceFixtures.seedMember(userHomes, "use0000002");
        HomePersistenceFixtures.seedMember(userHomes, "use0000003");
        flushAndClear();

        assertEquals(1, userHomes.countActiveOwnersByHomeId(HomePersistenceFixtures.HOME_ID));
    }

    @Test
    void countActiveOwnersExcludesSoftDeleted() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomePersistenceFixtures.seedOwner(userHomes);
        HomePersistenceFixtures.seedMember(userHomes, "use0000002");
        flushAndClear();

        // Remove the owner
        userHomes.remove(HomePersistenceFixtures.USER_ID, HomePersistenceFixtures.HOME_ID);
        flushAndClear();

        assertEquals(0, userHomes.countActiveOwnersByHomeId(HomePersistenceFixtures.HOME_ID));
    }

    @Test
    void listActiveByUser() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomePersistenceFixtures.seedOwner(userHomes);
        HomePersistenceFixtures.seedMember(userHomes, "use0000002");
        flushAndClear();

        var memberships = userHomes.listActiveByUser(HomePersistenceFixtures.USER_ID);
        assertEquals(1, memberships.size());
    }

    @Test
    void listActiveByHomeId() {
        HomePersistenceFixtures.seedHomeType(entityManager);
        HomePersistenceFixtures.seedHome(homes);
        HomePersistenceFixtures.seedOwner(userHomes);
        HomePersistenceFixtures.seedMember(userHomes, "use0000002");
        flushAndClear();

        var memberships = userHomes.listActiveByHomeId(HomePersistenceFixtures.HOME_ID);
        assertEquals(2, memberships.size());
    }
}
