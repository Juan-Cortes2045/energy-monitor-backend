package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.exception.HomeNotAccessibleException;
import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.port.in.AuthorizeDeviceRead;
import com.energymonitor.measurement.application.port.in.GetHomeConsumption;
import com.energymonitor.measurement.application.port.out.ConsumptionLevelPersistencePort;
import com.energymonitor.measurement.application.port.out.HomeAccessPort;
import com.energymonitor.measurement.application.port.out.HourlyEnergyRow;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.application.result.HomeConsumptionHistoryResult;
import com.energymonitor.measurement.application.result.HomeConsumptionHistoryResult.Bucket;
import com.energymonitor.measurement.application.result.HomeConsumptionHistoryResult.DeviceEnergy;
import com.energymonitor.measurement.application.result.HomeConsumptionSummaryResult;
import com.energymonitor.measurement.application.result.HomeConsumptionSummaryResult.DeviceConsumption;
import com.energymonitor.measurement.application.result.HomeConsumptionSummaryResult.HourPower;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Consumption of a home, computed from hourly aggregates of its devices.
 *
 * <p><strong>Energy.</strong> {@code storedEnergy} is the cumulative counter of the meter, so the
 * energy of an hour is its highest reading minus the highest reading of the previous hour (or
 * the last reading before the range, for the first one). A counter that goes backwards (meter
 * reset or replaced) restarts the sequence: that hour counts only its own spread. Energy is never
 * negative.
 *
 * <p><strong>Time zones.</strong> Hours are aggregated in UTC and then placed in local days and
 * months; this is exact for every zone with whole-hour offsets.
 *
 * <p>Read-only: no transaction boundary. No Spring annotations; registered by
 * {@code MeasurementServiceConfiguration} because it needs the freshness window from configuration.
 */
public class HomeConsumptionUseCase implements GetHomeConsumption, AuthorizeDeviceRead {

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MeasurementPersistencePort measurements;
    private final ConsumptionLevelPersistencePort levels;
    private final HomeAccessPort homes;
    private final Clock clock;
    private final Duration freshness;

    /**
     * @param freshness how old the latest reading of a device may be for it to count in the
     *                  current power; the same window after which the device is considered offline
     */
    public HomeConsumptionUseCase(MeasurementPersistencePort measurements, ConsumptionLevelPersistencePort levels,
                                  HomeAccessPort homes, Clock clock, Duration freshness) {
        this.measurements = measurements;
        this.levels = levels;
        this.homes = homes;
        this.clock = clock;
        this.freshness = freshness;
    }

    @Override
    public HomeConsumptionSummaryResult summary(String userId, String homeId, ZoneId zone) {
        requireMember(userId, homeId);
        Instant now = clock.instant();
        ZonedDateTime local = now.atZone(zone);
        Instant todayStart = local.toLocalDate().atStartOfDay(zone).toInstant();
        Instant monthStart = local.toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant firstHour = now.truncatedTo(ChronoUnit.HOURS).minus(23, ChronoUnit.HOURS);
        Instant from = monthStart.isBefore(firstHour) ? monthStart : firstHour;

        List<String> deviceIds = homes.deviceIds(homeId);
        Map<String, List<HourEnergy>> hourly = hourlyEnergy(deviceIds, from, now);

        double todayEnergy = 0;
        double monthEnergy = 0;
        Map<Instant, Double> powerByHour = new LinkedHashMap<>();
        List<DeviceConsumption> perDevice = new ArrayList<>();
        Double totalPower = null;

        for (String deviceId : deviceIds) {
            double deviceToday = 0;
            for (HourEnergy hour : hourly.getOrDefault(deviceId, List.of())) {
                if (!hour.start().isBefore(todayStart)) {
                    deviceToday += hour.energy();
                }
                if (!hour.start().isBefore(monthStart)) {
                    monthEnergy += hour.energy();
                }
                if (!hour.start().isBefore(firstHour)) {
                    powerByHour.merge(hour.start(), hour.averagePower(), Double::sum);
                }
            }
            todayEnergy += deviceToday;

            Double current = measurements.findLatestActiveByDevice(deviceId)
                    .filter(m -> !m.dateTime().isBefore(now.minus(freshness)))
                    .map(Measurement::getActivePower)
                    .orElse(null);
            if (current != null) {
                totalPower = (totalPower == null ? 0 : totalPower) + current;
            }
            perDevice.add(new DeviceConsumption(deviceId, current, round(deviceToday)));
        }

        List<HourPower> lastHours = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            Instant hour = firstHour.plus(i, ChronoUnit.HOURS);
            Double power = powerByHour.get(hour);
            lastHours.add(new HourPower(hour, power == null ? null : round(power)));
        }

        RiskConsumption level = totalPower == null ? null
                : levels.findLevelFor(totalPower).map(ConsumptionLevel::name).orElse(null);
        HomeAccessPort.Limits limits = homes.limits(homeId).orElse(null);

        return new HomeConsumptionSummaryResult(totalPower == null ? null : round(totalPower), level,
                round(todayEnergy), round(monthEnergy),
                limits == null ? null : limits.dailyLimit(), limits == null ? null : limits.monthlyLimit(),
                limits == null ? null : limits.limitPeriod(), lastHours, perDevice);
    }

    @Override
    public HomeConsumptionHistoryResult history(String userId, String homeId, Period period, ZoneId zone) {
        requireMember(userId, homeId);
        Instant now = clock.instant();
        LocalDate today = now.atZone(zone).toLocalDate();

        List<Instant> starts = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        Function<ZonedDateTime, String> keyOf;
        switch (period) {
            case DAY -> {
                ZonedDateTime start = today.atStartOfDay(zone);
                for (int h = 0; h < 24; h++) {
                    ZonedDateTime hour = start.plusHours(h);
                    starts.add(hour.toInstant());
                    keys.add(String.valueOf(hour.getHour()));
                }
                keyOf = t -> String.valueOf(t.getHour());
            }
            case WEEK -> {
                for (int d = 6; d >= 0; d--) {
                    LocalDate day = today.minusDays(d);
                    starts.add(day.atStartOfDay(zone).toInstant());
                    keys.add(day.format(DAY_KEY));
                }
                keyOf = t -> t.toLocalDate().format(DAY_KEY);
            }
            case MONTH -> {
                YearMonth month = YearMonth.from(today);
                for (int d = 1; d <= month.lengthOfMonth(); d++) {
                    LocalDate day = month.atDay(d);
                    starts.add(day.atStartOfDay(zone).toInstant());
                    keys.add(day.format(DAY_KEY));
                }
                keyOf = t -> t.toLocalDate().format(DAY_KEY);
            }
            case YEAR -> {
                for (int m = 1; m <= 12; m++) {
                    YearMonth month = YearMonth.of(today.getYear(), m);
                    starts.add(month.atDay(1).atStartOfDay(zone).toInstant());
                    keys.add(month.format(MONTH_KEY));
                }
                keyOf = t -> YearMonth.from(t).format(MONTH_KEY);
            }
            default -> throw new IllegalArgumentException("unknown period " + period);
        }

        Instant from = starts.getFirst();
        Instant previousFrom = switch (period) {
            case DAY -> today.minusDays(1).atStartOfDay(zone).toInstant();
            case WEEK -> today.minusDays(13).atStartOfDay(zone).toInstant();
            case MONTH -> today.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant();
            case YEAR -> today.withDayOfYear(1).minusYears(1).atStartOfDay(zone).toInstant();
        };
        List<String> deviceIds = homes.deviceIds(homeId);
        Map<String, List<HourEnergy>> hourly = hourlyEnergy(deviceIds, from, now);

        Map<String, Map<String, Double>> energyByKey = new LinkedHashMap<>();
        keys.forEach(key -> energyByKey.put(key, new LinkedHashMap<>()));
        hourly.forEach((deviceId, hours) -> hours.forEach(hour -> {
            Map<String, Double> bucket = energyByKey.get(keyOf.apply(hour.start().atZone(zone)));
            if (bucket != null) {
                bucket.merge(deviceId, hour.energy(), Double::sum);
            }
        }));

        List<Bucket> buckets = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            Map<String, Double> bucket = energyByKey.get(keys.get(i));
            List<DeviceEnergy> values = deviceIds.stream()
                    .map(id -> new DeviceEnergy(id, round(bucket.getOrDefault(id, 0.0))))
                    .toList();
            buckets.add(new Bucket(keys.get(i), starts.get(i), values));
        }
        double previousTotal = hourlyEnergy(deviceIds, previousFrom, from.minusMillis(1)).values().stream()
                .flatMap(List::stream)
                .mapToDouble(HourEnergy::energy)
                .sum();
        return new HomeConsumptionHistoryResult(period.name().toLowerCase(), from, now, buckets, round(previousTotal));
    }

    @Override
    public void requireReadable(String userId, String deviceId) {
        boolean readable = homes.homeOf(deviceId).map(homeId -> homes.isMember(userId, homeId)).orElse(false);
        if (!readable) {
            throw new MeasurementNotFoundException("no measurements for device " + deviceId);
        }
    }

    private void requireMember(String userId, String homeId) {
        if (!homes.isMember(userId, homeId)) {
            throw new HomeNotAccessibleException(homeId);
        }
    }

    private record HourEnergy(Instant start, double averagePower, double energy) {
    }

    /** Energy of every hour with readings, per device, oldest first. */
    private Map<String, List<HourEnergy>> hourlyEnergy(List<String> deviceIds, Instant from, Instant to) {
        Map<String, List<HourEnergy>> result = new LinkedHashMap<>();
        if (deviceIds.isEmpty()) {
            return result;
        }
        Map<String, Double> previous = new LinkedHashMap<>();
        for (String deviceId : deviceIds) {
            measurements.findLatestActiveByDeviceBefore(deviceId, from)
                    .ifPresent(m -> previous.put(deviceId, m.getStoredEnergy()));
        }
        for (HourlyEnergyRow row : measurements.aggregateHourly(deviceIds, from, to)) {
            Double before = previous.get(row.deviceId());
            double energy = before == null || row.maxStoredEnergy() < before
                    ? row.maxStoredEnergy() - row.minStoredEnergy()
                    : row.maxStoredEnergy() - before;
            previous.put(row.deviceId(), row.maxStoredEnergy());
            result.computeIfAbsent(row.deviceId(), id -> new ArrayList<>())
                    .add(new HourEnergy(row.hourStart(), row.averagePower(), Math.max(0, energy)));
        }
        return result;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
