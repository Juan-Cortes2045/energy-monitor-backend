package com.energymonitor.recommendation.domain.service;

import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.domain.model.ConsumptionLimit;
import com.energymonitor.recommendation.domain.model.HourSample;
import com.energymonitor.recommendation.domain.model.MonitoredDevice;
import com.energymonitor.recommendation.domain.model.Suggestion;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The rules that turn the hourly consumption of a home into suggestions. Pure: no I/O, no clock,
 * no framework, so every rule is tested with plain data.
 *
 * <ul>
 *   <li><strong>PEAK_HOURS</strong>: at least 40 % of the last 7 days' energy (minimum 2 kWh)
 *       was used between 18:00 and 22:00, local time.</li>
 *   <li><strong>SAVING</strong>: a device that is not always-on drew 15 W or more on average
 *       between 01:00 and 05:00 on each of the last 3 nights.</li>
 *   <li><strong>HISTORICAL_COMPARISON</strong>: the home used 25 % more in the last 7 days than
 *       in the 7 before (which must hold at least 1 kWh).</li>
 *   <li><strong>THRESHOLD</strong>: at the current pace the home exceeds its limit: the monthly
 *       projection of the month so far (from day 3 on), or the daily average of the last 7 days.</li>
 *   <li><strong>HIGH_CONSUMPTION</strong>: a device's mean power in the last 7 days is 30 % above
 *       its mean in the 21 days before.</li>
 * </ul>
 *
 * <p>Hours are UTC in the input; the day-part rules convert them to the home's zone.
 */
public class RecommendationRules {

    public static final String PEAK_HOURS_KEY = "recommendation.peakHours";
    public static final String STANDBY_KEY = "recommendation.standby";
    public static final String ABOVE_AVERAGE_KEY = "recommendation.aboveAverage";
    public static final String LIMIT_PROJECTION_KEY = "recommendation.limitProjection";
    public static final String DEVICE_INCREASE_KEY = "recommendation.deviceIncrease";

    static final double PEAK_SHARE = 0.40;
    static final double PEAK_MIN_KWH = 2.0;
    static final int PEAK_FROM_HOUR = 18;
    static final int PEAK_TO_HOUR = 22;

    static final double STANDBY_MIN_W = 15.0;
    static final int STANDBY_FROM_HOUR = 1;
    static final int STANDBY_TO_HOUR = 5;
    static final int STANDBY_NIGHTS = 3;
    static final int STANDBY_MIN_HOURS_PER_NIGHT = 2;

    static final double WEEK_INCREASE = 1.25;
    static final double WEEK_MIN_PREVIOUS_KWH = 1.0;

    static final int MONTH_MIN_ELAPSED_DAYS = 3;

    static final double DEVICE_INCREASE = 1.30;
    static final double DEVICE_MIN_BASELINE_W = 5.0;
    static final int DEVICE_MIN_RECENT_HOURS = 24;
    static final int DEVICE_MIN_BASELINE_HOURS = 72;

    private static final Duration WEEK = Duration.ofDays(7);

    /** Earliest instant the rules look at, so the caller fetches exactly what is needed. */
    public static Instant dataFrom(Instant now, ZoneId zone) {
        Instant fourWeeks = now.minus(Duration.ofDays(28));
        Instant monthStart = now.atZone(zone).toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant();
        return monthStart.isBefore(fourWeeks) ? monthStart : fourWeeks;
    }

    public List<Suggestion> evaluate(Instant now, ZoneId zone, Collection<MonitoredDevice> devices,
                                     List<HourSample> samples, Optional<ConsumptionLimit> limit) {
        List<Suggestion> out = new ArrayList<>();
        Instant weekAgo = now.minus(WEEK);
        List<HourSample> lastWeek = between(samples, weekAgo, now);

        peakHours(lastWeek, zone).ifPresent(out::add);
        for (MonitoredDevice device : devices) {
            if (!device.alwaysOn()) {
                standby(device.deviceId(), samples, now, zone).ifPresent(out::add);
            }
        }
        weekOverWeek(lastWeek, between(samples, weekAgo.minus(WEEK), weekAgo)).ifPresent(out::add);
        limit.flatMap(l -> limitProjection(l, samples, now, zone)).ifPresent(out::add);
        Map<String, List<HourSample>> byDevice = samples.stream()
                .collect(Collectors.groupingBy(HourSample::deviceId));
        for (MonitoredDevice device : devices) {
            deviceIncrease(device.deviceId(), byDevice.getOrDefault(device.deviceId(), List.of()), now)
                    .ifPresent(out::add);
        }
        return out;
    }

    Optional<Suggestion> peakHours(List<HourSample> lastWeek, ZoneId zone) {
        double total = energy(lastWeek, s -> true);
        if (total < PEAK_MIN_KWH) {
            return Optional.empty();
        }
        double peak = energy(lastWeek, s -> {
            int hour = s.hourStart().atZone(zone).getHour();
            return hour >= PEAK_FROM_HOUR && hour < PEAK_TO_HOUR;
        });
        return peak / total >= PEAK_SHARE
                ? Optional.of(new Suggestion(RecommendationType.PEAK_HOURS, null, PEAK_HOURS_KEY))
                : Optional.empty();
    }

    Optional<Suggestion> standby(String deviceId, List<HourSample> samples, Instant now, ZoneId zone) {
        ZonedDateTime local = now.atZone(zone);
        // The night of "today" counts only once it is over.
        LocalDate lastNight = local.getHour() >= STANDBY_TO_HOUR
                ? local.toLocalDate() : local.toLocalDate().minusDays(1);
        for (int i = 0; i < STANDBY_NIGHTS; i++) {
            LocalDate night = lastNight.minusDays(i);
            List<Double> powers = samples.stream()
                    .filter(s -> s.deviceId().equals(deviceId))
                    .filter(s -> {
                        ZonedDateTime t = s.hourStart().atZone(zone);
                        return t.toLocalDate().equals(night)
                                && t.getHour() >= STANDBY_FROM_HOUR && t.getHour() < STANDBY_TO_HOUR;
                    })
                    .map(HourSample::averagePower)
                    .toList();
            if (powers.size() < STANDBY_MIN_HOURS_PER_NIGHT
                    || mean(powers) < STANDBY_MIN_W) {
                return Optional.empty();
            }
        }
        return Optional.of(new Suggestion(RecommendationType.SAVING, deviceId, STANDBY_KEY));
    }

    Optional<Suggestion> weekOverWeek(List<HourSample> lastWeek, List<HourSample> previousWeek) {
        double previous = energy(previousWeek, s -> true);
        if (previous < WEEK_MIN_PREVIOUS_KWH) {
            return Optional.empty();
        }
        return energy(lastWeek, s -> true) >= previous * WEEK_INCREASE
                ? Optional.of(new Suggestion(RecommendationType.HISTORICAL_COMPARISON, null, ABOVE_AVERAGE_KEY))
                : Optional.empty();
    }

    Optional<Suggestion> limitProjection(ConsumptionLimit limit, List<HourSample> samples, Instant now,
                                         ZoneId zone) {
        if (limit.kwh() <= 0) {
            return Optional.empty();
        }
        double projected;
        if (limit.period() == ConsumptionLimit.Period.MONTHLY) {
            LocalDate today = now.atZone(zone).toLocalDate();
            Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
            double elapsedDays = Duration.between(monthStart, now).toMinutes() / 1440.0;
            if (elapsedDays < MONTH_MIN_ELAPSED_DAYS) {
                return Optional.empty();
            }
            double soFar = energy(between(samples, monthStart, now), s -> true);
            projected = soFar / elapsedDays * YearMonth.from(today).lengthOfMonth();
        } else {
            List<HourSample> lastWeek = between(samples, now.minus(WEEK), now);
            if (lastWeek.isEmpty()) {
                return Optional.empty();
            }
            Instant first = lastWeek.stream().map(HourSample::hourStart).min(Instant::compareTo).orElseThrow();
            double days = Math.max(1.0, Duration.between(first, now).toMinutes() / 1440.0);
            projected = energy(lastWeek, s -> true) / days;
        }
        return projected > limit.kwh()
                ? Optional.of(new Suggestion(RecommendationType.THRESHOLD, null, LIMIT_PROJECTION_KEY))
                : Optional.empty();
    }

    Optional<Suggestion> deviceIncrease(String deviceId, List<HourSample> deviceSamples, Instant now) {
        Instant weekAgo = now.minus(WEEK);
        List<Double> recent = powers(between(deviceSamples, weekAgo, now));
        List<Double> baseline = powers(between(deviceSamples, weekAgo.minus(Duration.ofDays(21)), weekAgo));
        if (recent.size() < DEVICE_MIN_RECENT_HOURS || baseline.size() < DEVICE_MIN_BASELINE_HOURS) {
            return Optional.empty();
        }
        double before = mean(baseline);
        if (before < DEVICE_MIN_BASELINE_W) {
            return Optional.empty();
        }
        return mean(recent) >= before * DEVICE_INCREASE
                ? Optional.of(new Suggestion(RecommendationType.HIGH_CONSUMPTION, deviceId, DEVICE_INCREASE_KEY))
                : Optional.empty();
    }

    private static List<HourSample> between(List<HourSample> samples, Instant from, Instant to) {
        return samples.stream()
                .filter(s -> !s.hourStart().isBefore(from) && s.hourStart().isBefore(to))
                .toList();
    }

    private static double energy(List<HourSample> samples, Predicate<HourSample> filter) {
        return samples.stream().filter(filter).mapToDouble(HourSample::energy).sum();
    }

    private static List<Double> powers(List<HourSample> samples) {
        return samples.stream().map(HourSample::averagePower).toList();
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
