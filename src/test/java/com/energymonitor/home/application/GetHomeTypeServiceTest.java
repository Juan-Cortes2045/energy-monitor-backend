package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.home.application.command.GetHomeTypeQuery;
import com.energymonitor.home.application.exception.HomeTypeNotFoundException;
import com.energymonitor.home.application.usecase.GetHomeTypeService;
import com.energymonitor.home.domain.model.HomeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetHomeTypeServiceTest {

    private HomeUseCaseFixtures.FakeHomeTypePersistencePort homeTypePort;
    private GetHomeTypeService service;

    @BeforeEach
    void setUp() {
        homeTypePort = new HomeUseCaseFixtures.FakeHomeTypePersistencePort();
        homeTypePort.seed(new HomeType("hous000001", "house"));
        service = new GetHomeTypeService(homeTypePort);
    }

    @Test
    void getHomeTypeWithValidId() {
        var query = new GetHomeTypeQuery("hous000001");
        var result = service.get(query);

        assertEquals("hous000001", result.idHomeType());
        assertEquals("house", result.name());
    }

    @Test
    void getHomeTypeWithInvalidIdThrows() {
        var query = new GetHomeTypeQuery("inva000001");
        assertThrows(HomeTypeNotFoundException.class, () -> service.get(query));
    }
}
