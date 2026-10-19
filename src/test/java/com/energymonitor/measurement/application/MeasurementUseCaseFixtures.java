package com.energymonitor.measurement.application;

import com.energymonitor.measurement.api.MeasurementRecorded;
import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.port.out.ConsumptionLevelPersistencePort;
import com.energymonitor.measurement.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.measurement.application.port.out.HourlyEnergyRow;
import com.energymonitor.measurement.application.port.out.MeasurementEventPort;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory fakes of the output ports, for application-layer tests only.
 *
 * <p>They replace the {@code @Component} persistence and event adapters so the use cases
 * can be exercised without a database or a Spring context.
 */
public final class MeasurementUseCaseFixtures {

    public static final Instant NOW = Instant.parse("2026-01-15T10:30:00Z");

    private MeasurementUseCaseFixtures() {
    }

    /** A clock fixed at {@link #NOW}. */
    public static Clock clock() {
        return Clock.fixed(NOW, ZoneId.of("UTC"));
    }

    public static class FakeMeasurementPersistencePort implements MeasurementPersistencePort {

        private final Map<String, Measurement> byId = new LinkedHashMap<>();

        @Override
        public Measurement save(Measurement measurement) {
            byId.put(measurement.idMeasurement(), measurement);
            return measurement;
        }

        @Override
        public Optional<Measurement> findActive(String idMeasurement) {
            return Optional.ofNullable(byId.get(idMeasurement));
        }

        @Override
        public List<Measurement> listActiveByDevice(String deviceId, Instant from, Instant to) {
            return byId.values().stream()
                    .filter(m -> m.deviceId().equals(deviceId))
                    .filter(m -> !m.dateTime().isBefore(from) && !m.dateTime().isAfter(to))
                    .sorted(Comparator.comparing(Measurement::dateTime))
                    .toList();
        }

        @Override
        public Optional<Measurement> findLatestActiveByDevice(String deviceId) {
            return byId.values().stream()
                    .filter(m -> m.deviceId().equals(deviceId))
                    .max(Comparator.comparing(Measurement::dateTime));
        }

        @Override
        public List<HourlyEnergyRow> aggregateHourly(Collection<String> deviceIds, Instant from, Instant to) {
            Map<String, List<Measurement>> groups = new TreeMap<>();
            byId.values().stream()
                    .filter(m -> deviceIds.contains(m.deviceId()))
                    .filter(m -> !m.dateTime().isBefore(from) && !m.dateTime().isAfter(to))
                    .forEach(m -> groups.computeIfAbsent(
                            m.deviceId() + "|" + m.dateTime().truncatedTo(ChronoUnit.HOURS), k -> new ArrayList<>())
                            .add(m));
            return groups.values().stream()
                    .map(list -> new HourlyEnergyRow(list.getFirst().deviceId(),
                            list.getFirst().dateTime().truncatedTo(ChronoUnit.HOURS),
                            list.stream().mapToDouble(Measurement::getActivePower).average().orElseThrow(),
                            list.stream().mapToDouble(Measurement::getStoredEnergy).min().orElseThrow(),
                            list.stream().mapToDouble(Measurement::getStoredEnergy).max().orElseThrow()))
                    .toList();
        }

        @Override
        public Optional<Measurement> findLatestActiveByDeviceBefore(String deviceId, Instant before) {
            return byId.values().stream()
                    .filter(m -> m.deviceId().equals(deviceId) && m.dateTime().isBefore(before))
                    .max(Comparator.comparing(Measurement::dateTime));
        }

        @Override
        public List<String> devicesReportingSince(Instant since) {
            return byId.values().stream()
                    .filter(m -> !m.dateTime().isBefore(since))
                    .map(Measurement::deviceId)
                    .distinct()
                    .toList();
        }

        public void seed(Measurement measurement) {
            byId.put(measurement.idMeasurement(), measurement);
        }
    }

    public static class FakeConsumptionLevelPersistencePort implements ConsumptionLevelPersistencePort {

        private final Map<String, ConsumptionLevel> byId = new LinkedHashMap<>();

        @Override
        public Optional<ConsumptionLevel> findActiveByName(RiskConsumption name) {
            return byId.values().stream()
                    .filter(level -> level.name() == name)
                    .findFirst();
        }

        @Override
        public List<ConsumptionLevel> listActive() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public Optional<ConsumptionLevel> findLevelFor(double activePower) {
            return listActive().stream()
                    .filter(level -> level.contains(activePower))
                    .findFirst();
        }

        public void seed(ConsumptionLevel level) {
            byId.put(level.idConsumptionLevel(), level);
        }
    }

    public static class FakeIdentifierGeneratorPort implements IdentifierGeneratorPort {

        private int sequence;

        @Override
        public String generate() {
            sequence++;
            return "id" + String.format("%07d", sequence);
        }
    }

    public static class FakeMeasurementEventPort implements MeasurementEventPort {

        public final List<MeasurementRecorded> published = new ArrayList<>();

        @Override
        public void publish(MeasurementRecorded event) {
            published.add(event);
        }
    }
}
