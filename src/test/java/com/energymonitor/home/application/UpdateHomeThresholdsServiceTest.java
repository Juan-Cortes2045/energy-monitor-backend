package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.home.application.command.UpdateHomeThresholdsCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.usecase.UpdateHomeThresholdsService;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateHomeThresholdsServiceTest {

    private HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort thresholdsPort;
    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private UpdateHomeThresholdsService service;

    @BeforeEach
    void setUp() {
        thresholdsPort = new HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort();
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();

        thresholdsPort.save(HomeThresholds.create("thr0000001", "hom0000001", 10.0, 300.0, true));
        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));
        userHomePort.save(UserHome.member("use0000002", "hom0000001"));

        service = new UpdateHomeThresholdsService(thresholdsPort, userHomePort);
    }

    @Test
    void updateThresholdsWithOwnerPermission() {
        var command = new UpdateHomeThresholdsCommand("use0000001", "hom0000001", 15.0, 450.0);
        var result = service.update(command);

        assertEquals(15.0, result.dailyLimit());
        assertEquals(450.0, result.monthlyLimit());
        assertFalse(result.useSystemDefault());
    }

    @Test
    void updateThresholdsWithoutMembershipThrows404() {
        var command = new UpdateHomeThresholdsCommand("use0000003", "hom0000001", 15.0, 450.0);
        assertThrows(HomeNotFoundException.class, () -> service.update(command));
    }

    @Test
    void updateThresholdsAsMemberWithoutPermissionThrows403() {
        var command = new UpdateHomeThresholdsCommand("use0000002", "hom0000001", 15.0, 450.0);
        assertThrows(HomeAccessDeniedException.class, () -> service.update(command));
    }

    @Test
    void updateThresholdsWithInvalidValuesThrows() {
        var command = new UpdateHomeThresholdsCommand("use0000001", "hom0000001", 500.0, 300.0);
        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }
}
