package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.application.command.ListHomeTypesQuery;
import com.energymonitor.home.application.usecase.ListHomeTypesService;
import com.energymonitor.home.domain.model.HomeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListHomeTypesServiceTest {

    private HomeUseCaseFixtures.FakeHomeTypePersistencePort homeTypePort;
    private ListHomeTypesService service;

    @BeforeEach
    void setUp() {
        homeTypePort = new HomeUseCaseFixtures.FakeHomeTypePersistencePort();

        homeTypePort.seed(new HomeType("hous000001", "house"));
        homeTypePort.seed(new HomeType("apar000001", "apartment"));
        homeTypePort.seed(new HomeType("stud000001", "studio"));
        homeTypePort.seed(new HomeType("othe000001", "other"));

        service = new ListHomeTypesService(homeTypePort);
    }

    @Test
    void listHomeTypesReturnsAllTypes() {
        var query = new ListHomeTypesQuery();
        var results = service.list(query);

        assertEquals(4, results.size());
    }

    @Test
    void listHomeTypesReturnsEmptyWhenNoTypes() {
        HomeUseCaseFixtures.FakeHomeTypePersistencePort emptyPort = new HomeUseCaseFixtures.FakeHomeTypePersistencePort();
        ListHomeTypesService emptyService = new ListHomeTypesService(emptyPort);

        var results = emptyService.list(new ListHomeTypesQuery());
        assertTrue(results.isEmpty());
    }
}
