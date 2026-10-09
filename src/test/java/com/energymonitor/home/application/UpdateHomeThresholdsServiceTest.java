package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.api.LimitPeriod;
import com.energymonitor.home.application.command.UpdateHomeThresholdsCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.port.out.SystemDefaultsPort;
import com.energymonitor.home.application.usecase.UpdateHomeThresholdsService;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateHomeThresholdsServiceTest {

    static final SystemDefaultsPort DEFAULTS = new SystemDefaultsPort() {
        @Override
        public double defaultDailyLimit() {
            return 6.0;
        }

        @Override
        public double defaultMonthlyLimit() {
            return 180.0;
        }
    };

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

        service = new UpdateHomeThresholdsService(thresholdsPort, userHomePort, DEFAULTS);
    }

    @Test
    void updateThresholdsWithOwnerPermission() {
        var command = new UpdateHomeThresholdsCommand("use0000001", "hom0000001", LimitPeriod.DAILY, 15.0);
        var result = service.update(command);

        assertEquals(15.0, result.dailyLimit());
        assertEquals(450.0, result.monthlyLimit());
        assertEquals(LimitPeriod.DAILY, result.limitPeriod());
        assertFalse(result.useSystemDefault());
    }

    @Test
    void settingTheMonthlyLimitDerivesTheDailyOne() {
        var result = service.update(new UpdateHomeThresholdsCommand("use0000001", "hom0000001",
                LimitPeriod.MONTHLY, 600.0));

        assertEquals(600.0, result.monthlyLimit());
        assertEquals(20.0, result.dailyLimit());
        assertEquals(LimitPeriod.MONTHLY, result.limitPeriod());
    }

    @Test
    void updateThresholdsWithoutMembershipThrows404() {
        var command = new UpdateHomeThresholdsCommand("use0000003", "hom0000001", LimitPeriod.DAILY, 15.0);
        assertThrows(HomeNotFoundException.class, () -> service.update(command));
    }

    @Test
    void updateThresholdsAsMemberWithoutPermissionThrows403() {
        var command = new UpdateHomeThresholdsCommand("use0000002", "hom0000001", LimitPeriod.DAILY, 15.0);
        assertThrows(HomeAccessDeniedException.class, () -> service.update(command));
    }

    @Test
    void updateThresholdsWithInvalidValuesThrows() {
        var command = new UpdateHomeThresholdsCommand("use0000001", "hom0000001", LimitPeriod.MONTHLY, -5.0);
        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }

    @Test
    void resettingAppliesTheSystemDefaults() {
        service.update(new UpdateHomeThresholdsCommand("use0000001", "hom0000001", LimitPeriod.MONTHLY, 600.0));

        var result = service.resetToDefaults("use0000001", "hom0000001");

        assertEquals(6.0, result.dailyLimit());
        assertEquals(180.0, result.monthlyLimit());
        assertEquals(LimitPeriod.DAILY, result.limitPeriod());
        assertTrue(result.useSystemDefault());
        assertEquals(6.0, result.defaultDailyLimit());
        assertEquals(180.0, result.defaultMonthlyLimit());
    }

    @Test
    void onlyManagersResetAndOutsidersSeeNothing() {
        assertThrows(HomeAccessDeniedException.class, () -> service.resetToDefaults("use0000002", "hom0000001"));
        assertThrows(HomeNotFoundException.class, () -> service.resetToDefaults("use0000003", "hom0000001"));
    }
}
