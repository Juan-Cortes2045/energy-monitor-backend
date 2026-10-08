package com.energymonitor.measurement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.exception.HomeNotAccessibleException;
import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.port.in.GetHomeConsumption.Period;
import com.energymonitor.measurement.application.port.out.HomeAccessPort;
import com.energymonitor.measurement.application.result.HomeConsumptionHistoryResult;
import com.energymonitor.measurement.application.result.HomeConsumptionSummaryResult;
import com.energymonitor.measurement.application.usecase.HomeConsumptionUseCase;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HomeConsumptionUseCaseTest {

    /** 10:30 in Bogota (UTC-5). */
    private static final Instant NOW = Instant.parse("2026-01-15T15:30:00Z");
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String HOME = "home000001";
    private static final String MEMBER = "usr0000001";
    private static final String FRIDGE = "dev0000001";
    private static final String TV = "dev0000002";

    private MeasurementUseCaseFixtures.FakeMeasurementPersistencePort measurements;
    private HomeConsumptionUseCase useCase;
    private int ids;

    @BeforeEach
    void setUp() {
        measurements = new MeasurementUseCaseFixtures.FakeMeasurementPersistencePort();
        var levels = new MeasurementUseCaseFixtures.FakeConsumptionLevelPersistencePort();
        levels.seed(new ConsumptionLevel("low0000001", RiskConsumption.LOW, "low", 0, 500));
        levels.seed(new ConsumptionLevel("medi000001", RiskConsumption.MEDIUM, "medium", 500, 1500));
        HomeAccessPort homes = new HomeAccessPort() {
            @Override
            public boolean isMember(String userId, String homeId) {
                return MEMBER.equals(userId) && HOME.equals(homeId);
            }

            @Override
            public List<String> deviceIds(String homeId) {
                return List.of(FRIDGE, TV);
            }

            @Override
            public Optional<String> homeOf(String deviceId) {
                return Map.of(FRIDGE, HOME, TV, HOME).containsKey(deviceId) ? Optional.of(HOME) : Optional.empty();
            }

            @Override
            public Optional<Limits> limits(String homeId) {
                return Optional.of(new Limits(10, 300, "DAILY"));
            }
        };
        useCase = new HomeConsumptionUseCase(measurements, levels, homes, Clock.fixed(NOW, ZoneId.of("UTC")),
                Duration.ofSeconds(150));
    }

    private void reading(String device, String at, double power, double energy) {
        measurements.seed(Measurement.record(String.format("mea%07d", ++ids), device, Instant.parse(at),
                120, power / 120, power, energy));
    }

    @Test
    void energyIsTheGrowthOfTheCounterSinceThePreviousReading() {
        reading(FRIDGE, "2026-01-15T04:50:00Z", 100, 10.0);   // 23:50 yesterday in Bogota: baseline
        reading(FRIDGE, "2026-01-15T05:10:00Z", 100, 10.2);   // 00:10 today
        reading(FRIDGE, "2026-01-15T14:00:00Z", 120, 11.0);
        reading(FRIDGE, "2026-01-15T15:29:30Z", 150, 11.5);   // fresh

        HomeConsumptionSummaryResult summary = useCase.summary(MEMBER, HOME, BOGOTA);

        assertEquals(1.5, summary.todayEnergy(), 1e-9);
        assertEquals(150.0, summary.currentPower());
        assertEquals(RiskConsumption.LOW, summary.level());
        assertEquals(10.0, summary.dailyLimit());
        assertEquals(24, summary.lastHours().size());
        assertEquals(150.0, summary.lastHours().getLast().averagePower());
    }

    @Test
    void aStaleDeviceDoesNotCountInTheCurrentPower() {
        reading(FRIDGE, "2026-01-15T15:29:30Z", 400, 1.0);
        reading(TV, "2026-01-15T15:20:00Z", 900, 2.0);        // 10 minutes old

        HomeConsumptionSummaryResult summary = useCase.summary(MEMBER, HOME, BOGOTA);

        assertEquals(400.0, summary.currentPower());
        assertNull(summary.devices().get(1).currentPower());
    }

    @Test
    void noReadingsMeansNoCurrentPowerAndNoLevel() {
        HomeConsumptionSummaryResult summary = useCase.summary(MEMBER, HOME, BOGOTA);

        assertNull(summary.currentPower());
        assertNull(summary.level());
        assertEquals(0.0, summary.todayEnergy());
    }

    @Test
    void aCounterResetNeverProducesNegativeEnergy() {
        reading(FRIDGE, "2026-01-15T13:10:00Z", 100, 50.0);
        reading(FRIDGE, "2026-01-15T14:05:00Z", 100, 0.1);    // meter reset
        reading(FRIDGE, "2026-01-15T14:55:00Z", 100, 0.4);

        HomeConsumptionSummaryResult summary = useCase.summary(MEMBER, HOME, BOGOTA);

        assertEquals(0.3, summary.todayEnergy(), 1e-9);
    }

    @Test
    void dayHistoryHasOneBucketPerLocalHour() {
        reading(TV, "2026-01-15T14:10:00Z", 100, 5.0);         // 09:10 local
        reading(TV, "2026-01-15T14:50:00Z", 100, 5.25);

        HomeConsumptionHistoryResult history = useCase.history(MEMBER, HOME, Period.DAY, BOGOTA);

        assertEquals(24, history.buckets().size());
        assertEquals("9", history.buckets().get(9).key());
        assertEquals(0.25, history.buckets().get(9).devices().get(1).energy(), 1e-9);
    }

    @Test
    void historyComparesWithThePreviousPeriod() {
        reading(TV, "2026-01-14T14:10:00Z", 100, 4.0);         // yesterday
        reading(TV, "2026-01-14T15:10:00Z", 100, 4.5);
        reading(TV, "2026-01-15T14:10:00Z", 100, 5.0);         // today

        HomeConsumptionHistoryResult history = useCase.history(MEMBER, HOME, Period.DAY, BOGOTA);

        assertEquals(0.5, history.previousTotal(), 1e-9);
    }

    @Test
    void yearHistoryHasTwelveMonths() {
        HomeConsumptionHistoryResult history = useCase.history(MEMBER, HOME, Period.YEAR, BOGOTA);

        assertEquals(12, history.buckets().size());
        assertEquals("2026-01", history.buckets().getFirst().key());
    }

    @Test
    void outsidersCannotReadTheHome() {
        assertThrows(HomeNotAccessibleException.class, () -> useCase.summary("intruder", HOME, BOGOTA));
        assertThrows(MeasurementNotFoundException.class, () -> useCase.requireReadable("intruder", FRIDGE));
        assertThrows(MeasurementNotFoundException.class, () -> useCase.requireReadable(MEMBER, "dev0000099"));
    }
}
