package com.energymonitor.home.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.energymonitor.home.application.port.in.DescribeHomeAccess.HomeAccess;
import com.energymonitor.home.application.usecase.DescribeHomeAccessService;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Home access asked by the device service")
class DescribeHomeAccessServiceTest {

    private static final String HOME = "hom0000001";
    private static final String OWNER = "use0000001";
    private static final String MEMBER = "use0000002";
    private static final String STRANGER = "use0000003";

    private HomeUseCaseFixtures.FakeHomePersistencePort homes;
    private HomeUseCaseFixtures.FakeUserHomePersistencePort memberships;
    private DescribeHomeAccessService service;

    @BeforeEach
    void setUp() {
        homes = new HomeUseCaseFixtures.FakeHomePersistencePort();
        memberships = new HomeUseCaseFixtures.FakeUserHomePersistencePort();
        service = new DescribeHomeAccessService(homes, memberships);
        homes.save(Home.create(HOME, "Casa", "hous000001", "Calle 1", "AAA11111", null,
                HomeUseCaseFixtures.NOW));
        memberships.save(UserHome.owner(OWNER, HOME));
        memberships.save(UserHome.member(MEMBER, HOME));
    }

    @Test
    @DisplayName("an owner may change the devices")
    void owner() {
        assertThat(service.describe(OWNER, HOME)).isEqualTo(new HomeAccess(true, true, true));
    }

    @Test
    @DisplayName("a member may only list them")
    void member() {
        assertThat(service.describe(MEMBER, HOME)).isEqualTo(new HomeAccess(true, true, false));
    }

    @Test
    @DisplayName("somebody outside the home gets nothing, but learns it exists")
    void stranger() {
        assertThat(service.describe(STRANGER, HOME)).isEqualTo(new HomeAccess(true, false, false));
    }

    @Test
    @DisplayName("a home that does not exist grants nothing")
    void missingHome() {
        assertThat(service.describe(OWNER, "hom0000099")).isEqualTo(new HomeAccess(false, false, false));
    }

    @Test
    @DisplayName("no caller is nobody")
    void noCaller() {
        assertThat(service.describe(null, HOME)).isEqualTo(new HomeAccess(true, false, false));
        assertThat(service.describe(OWNER, null)).isEqualTo(new HomeAccess(false, false, false));
    }
}
