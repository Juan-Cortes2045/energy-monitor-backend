package com.energymonitor.recommendation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.domain.model.ConsumptionLimit;
import com.energymonitor.recommendation.domain.model.HourSample;
import com.energymonitor.recommendation.domain.model.MonitoredDevice;
import com.energymonitor.recommendation.domain.model.Suggestion;
import com.energymonitor.recommendation.domain.service.RecommendationRules;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;

class RecommendationRulesTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    /** Wednesday 20 May 2026, 12:00 in Bogotá. */
    private static final Instant NOW = ZonedDateTime.of(2026, 5, 20, 12, 0, 0, 0, ZONE).toInstant();
    private static final String DEV = "dev0000001";

    private final RecommendationRules rules = new RecommendationRules();

    /** One sample per hour for {@code days} days before NOW, power given by local hour. */
    private static List<HourSample> hours(String device, int fromDaysAgo, int toDaysAgo,
                                          ToDoubleFunction<ZonedDateTime> watts) {
        List<HourSample> out = new ArrayList<>();
        Instant t = NOW.minus(Duration.ofDays(fromDaysAgo));
        Instant end = NOW.minus(Duration.ofDays(toDaysAgo));
        for (; t.isBefore(end); t = t.plus(Duration.ofHours(1))) {
            double w = watts.applyAsDouble(t.atZone(ZONE));
            out.add(new HourSample(device, t, w, w / 1000.0));
        }
        return out;
    }

    private List<Suggestion> evaluate(List<MonitoredDevice> devices, List<HourSample> samples,
                                      Optional<ConsumptionLimit> limit) {
        return rules.evaluate(NOW, ZONE, devices, samples, limit);
    }

    private static boolean has(List<Suggestion> out, RecommendationType type) {
        return out.stream().anyMatch(s -> s.type() == type);
    }

    @Test
    void flatConsumptionSuggestsNothing() {
        var samples = hours(DEV, 28, 0, t -> 100);
        var out = evaluate(List.of(new MonitoredDevice(DEV, true)), samples, Optional.empty());
        assertTrue(out.isEmpty(), out.toString());
    }

    @Test
    void eveningPeakIsDetected() {
        var samples = hours(DEV, 7, 0, t -> t.getHour() >= 18 && t.getHour() < 22 ? 1500 : 50);
        var out = evaluate(List.of(new MonitoredDevice(DEV, true)), samples, Optional.empty());
        assertTrue(has(out, RecommendationType.PEAK_HOURS), out.toString());
    }

    @Test
    void nightStandbyIsDetectedExceptForAlwaysOnAppliances() {
        var samples = hours(DEV, 7, 0, t -> 30);
        var standby = evaluate(List.of(new MonitoredDevice(DEV, false)), samples, Optional.empty());
        assertEquals(DEV, standby.stream().filter(s -> s.type() == RecommendationType.SAVING)
                .findFirst().orElseThrow().deviceId());

        var fridge = evaluate(List.of(new MonitoredDevice(DEV, true)), samples, Optional.empty());
        assertTrue(!has(fridge, RecommendationType.SAVING), fridge.toString());
    }

    @Test
    void lowNightPowerIsNotStandby() {
        var samples = hours(DEV, 7, 0, t -> t.getHour() >= 1 && t.getHour() < 5 ? 3 : 200);
        var out = evaluate(List.of(new MonitoredDevice(DEV, false)), samples, Optional.empty());
        assertTrue(!has(out, RecommendationType.SAVING), out.toString());
    }

    @Test
    void weekOverWeekIncreaseIsDetected() {
        var samples = new ArrayList<>(hours(DEV, 14, 7, t -> 100));
        samples.addAll(hours(DEV, 7, 0, t -> 140));
        var out = evaluate(List.of(new MonitoredDevice(DEV, true)), samples, Optional.empty());
        assertTrue(has(out, RecommendationType.HISTORICAL_COMPARISON), out.toString());
    }

    @Test
    void deviceIncreaseNeedsThreeWeeksOfBaseline() {
        var samples = new ArrayList<>(hours(DEV, 28, 7, t -> 100));
        samples.addAll(hours(DEV, 7, 0, t -> 140));
        var out = evaluate(List.of(new MonitoredDevice(DEV, true)), samples, Optional.empty());
        assertTrue(has(out, RecommendationType.HIGH_CONSUMPTION), out.toString());

        var shortHistory = new ArrayList<>(hours(DEV, 9, 7, t -> 100));
        shortHistory.addAll(hours(DEV, 7, 0, t -> 140));
        var none = evaluate(List.of(new MonitoredDevice(DEV, true)), shortHistory, Optional.empty());
        assertTrue(!has(none, RecommendationType.HIGH_CONSUMPTION), none.toString());
    }

    @Test
    void dailyLimitIsComparedWithTheDailyAverage() {
        var samples = hours(DEV, 7, 0, t -> 500); // 12 kWh a day
        var over = evaluate(List.of(new MonitoredDevice(DEV, true)), samples,
                Optional.of(new ConsumptionLimit(ConsumptionLimit.Period.DAILY, 10)));
        assertTrue(has(over, RecommendationType.THRESHOLD), over.toString());

        var under = evaluate(List.of(new MonitoredDevice(DEV, true)), samples,
                Optional.of(new ConsumptionLimit(ConsumptionLimit.Period.DAILY, 15)));
        assertTrue(!has(under, RecommendationType.THRESHOLD), under.toString());
    }

    @Test
    void monthlyLimitIsComparedWithTheProjection() {
        var samples = hours(DEV, 28, 0, t -> 500); // 12 kWh a day => ~372 kWh in May
        var over = evaluate(List.of(new MonitoredDevice(DEV, true)), samples,
                Optional.of(new ConsumptionLimit(ConsumptionLimit.Period.MONTHLY, 300)));
        assertTrue(has(over, RecommendationType.THRESHOLD), over.toString());

        var under = evaluate(List.of(new MonitoredDevice(DEV, true)), samples,
                Optional.of(new ConsumptionLimit(ConsumptionLimit.Period.MONTHLY, 450)));
        assertTrue(!has(under, RecommendationType.THRESHOLD), under.toString());
    }
}
