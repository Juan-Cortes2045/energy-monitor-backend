package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.application.command.GetHomeThresholdsQuery;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.usecase.GetHomeThresholdsService;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetHomeThresholdsServiceTest {

    private HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort thresholdsPort;
    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private GetHomeThresholdsService service;

    @BeforeEach
    void setUp() {
        thresholdsPort = new HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort();
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();

        thresholdsPort.save(HomeThresholds.create("thr0000001", "hom0000001", 10.0, 300.0, true));
        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));

        service = new GetHomeThresholdsService(thresholdsPort, userHomePort);
    }

    @Test
    void getThresholdsWithValidMembership() {
        var query = new GetHomeThresholdsQuery("use0000001", "hom0000001");
        var result = service.get(query);

        assertEquals("thr0000001", result.idThreshold());
        assertEquals("hom0000001", result.homeId());
        assertEquals(10.0, result.dailyLimit());
        assertEquals(300.0, result.monthlyLimit());
        assertTrue(result.useSystemDefault());
    }

    @Test
    void getThresholdsWithoutMembershipThrows404() {
        var query = new GetHomeThresholdsQuery("use0000002", "hom0000001");
        assertThrows(HomeNotFoundException.class, () -> service.get(query));
    }
}
