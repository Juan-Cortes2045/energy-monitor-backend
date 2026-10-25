package com.energymonitor.home.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.energymonitor.home.api.LimitPeriod;
import com.energymonitor.home.application.port.in.DescribeHomeLimits.HomeLimits;
import com.energymonitor.home.application.usecase.DescribeHomeLimitsService;
import com.energymonitor.home.domain.model.HomeThresholds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Home limits asked by the measurement service")
class DescribeHomeLimitsServiceTest {

    private static final String HOME = "hom0000001";

    private HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort thresholds;
    private DescribeHomeLimitsService service;

    @BeforeEach
    void setUp() {
        thresholds = new HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort();
        service = new DescribeHomeLimitsService(thresholds);
    }

    @Test
    @DisplayName("the limits of a home, with the one the owner set")
    void limits() {
        HomeThresholds set = HomeThresholds.create("thr0000001", HOME, 10.0, 300.0, false);
        set.setLimit(LimitPeriod.MONTHLY, 300.0);
        thresholds.save(set);

        assertThat(service.describe(HOME))
                .contains(new HomeLimits(set.dailyLimit(), 300.0, LimitPeriod.MONTHLY));
    }

    @Test
    @DisplayName("a home without thresholds, or no home at all, has no limits")
    void none() {
        assertThat(service.describe("hom0000099")).isEmpty();
        assertThat(service.describe(null)).isEmpty();
    }

    @Test
    @DisplayName("the limits of a deleted home are gone with it")
    void deletedHome() {
        thresholds.save(HomeThresholds.create("thr0000001", HOME, 10.0, 300.0, true));
        thresholds.removeByHomeId(HOME);

        assertThat(service.describe(HOME)).isEmpty();
    }
}
