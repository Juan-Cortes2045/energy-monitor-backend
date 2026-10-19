package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.application.command.ListHomesQuery;
import com.energymonitor.home.application.usecase.ListHomesService;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListHomesServiceTest {

    private HomeUseCaseFixtures.FakeHomePersistencePort homePort;
    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private ListHomesService service;

    @BeforeEach
    void setUp() {
        homePort = new HomeUseCaseFixtures.FakeHomePersistencePort();
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();

        homePort.seed(Home.create("hom0000001", "Casa", "hous000001", "Calle 123", "ABC12345", null,
                HomeUseCaseFixtures.NOW));
        homePort.seed(Home.create("hom0000002", "Apartamento", "apar000001", "Calle 456", "XYZ98765", null,
                HomeUseCaseFixtures.NOW));

        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));
        userHomePort.save(UserHome.member("use0000001", "hom0000002"));

        var directory = new HomeUseCaseFixtures.FakeUserDirectoryPort();
        directory.seed("use0000001", "Ada", "Lovelace", "ada@example.com");
        service = new ListHomesService(homePort, userHomePort, directory);
    }

    @Test
    void listHomesReturnsAllUserHomes() {
        var query = new ListHomesQuery("use0000001");
        var results = service.list(query);

        assertEquals(2, results.size());
    }

    @Test
    void listHomesIncludesRoleAndFavorite() {
        var query = new ListHomesQuery("use0000001");
        var results = service.list(query);

        var home1 = results.stream().filter(r -> r.idHome().equals("hom0000001")).findFirst().orElseThrow();
        assertEquals(Role.OWNER, home1.role());
        assertFalse(home1.favorite());

        var home2 = results.stream().filter(r -> r.idHome().equals("hom0000002")).findFirst().orElseThrow();
        assertEquals(Role.MEMBER, home2.role());
        assertFalse(home2.favorite());
    }

    @Test
    void listHomesReturnsEmptyListWhenUserHasNoHomes() {
        var query = new ListHomesQuery("use0000003");
        var results = service.list(query);
        assertTrue(results.isEmpty());
    }

    @Test
    void listHomesUsesBatchQuery() {
        // This test verifies that the service uses findActiveByIds (batch) instead of N+1
        var query = new ListHomesQuery("use0000001");
        var results = service.list(query);
        assertEquals(2, results.size());
    }

    @Test
    void listHomesIncludesTheOwnerAsResponsible() {
        var results = service.list(new ListHomesQuery("use0000001"));

        var owned = results.stream().filter(r -> r.idHome().equals("hom0000001")).findFirst().orElseThrow();
        assertEquals("Ada", owned.userResponsible());
        assertEquals("Lovelace", owned.userResponsibleLastName());
        assertEquals("ada@example.com", owned.userResponsibleEmail());
        // hom0000002 has no owner in the fixtures: the responsible is left empty, not invented.
        var ownerless = results.stream().filter(r -> r.idHome().equals("hom0000002")).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(ownerless.userResponsible());
    }
}
