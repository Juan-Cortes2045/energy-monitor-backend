package com.energymonitor.home.adapter.out.persistence;

import com.energymonitor.home.adapter.out.persistence.entity.HomeTypeEntity;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.HomeType;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import jakarta.persistence.EntityManager;
import java.time.Instant;

/**
 * Shared seeds for persistence tests.
 */
final class HomePersistenceFixtures {

    static final String HOME_TYPE_ID = "hous000001";
    static final String HOME_ID = "hom0000001";
    static final String USER_ID = "use0000001";
    static final Instant CREATION_DATE = Instant.parse("2026-01-15T10:30:00Z");

    private HomePersistenceFixtures() {
    }

    static HomeType seedHomeType(HomeTypePersistenceAdapter homeTypes) {
        HomeType type = new HomeType(HOME_TYPE_ID, "house");
        // Note: HomeType is read-only in the catalog, but we need to seed it for tests
        // Since there's no save method in HomeTypePersistencePort, we use the repository directly
        return type;
    }

    static void seedHomeType(EntityManager entityManager) {
        HomeTypeEntity entity = new HomeTypeEntity(HOME_TYPE_ID, "house");
        entityManager.persist(entity);
        entityManager.flush();
    }

    static Home seedHome(HomePersistenceAdapter homes) {
        Home home = Home.create(HOME_ID, "Casa", HOME_TYPE_ID, "Calle 123", "ABC12345", "Mi casa", CREATION_DATE);
        homes.save(home);
        return home;
    }

    static HomeThresholds seedThresholds(HomeThresholdsPersistenceAdapter thresholds) {
        HomeThresholds t = HomeThresholds.create("thr0000001", HOME_ID, 10.0, 300.0, true);
        thresholds.save(t);
        return t;
    }

    static UserHome seedOwner(UserHomePersistencePort userHomes) {
        UserHome membership = UserHome.owner(USER_ID, HOME_ID);
        userHomes.save(membership);
        return membership;
    }

    static UserHome seedMember(UserHomePersistencePort userHomes, String userId) {
        UserHome membership = UserHome.member(userId, HOME_ID);
        userHomes.save(membership);
        return membership;
    }
}
