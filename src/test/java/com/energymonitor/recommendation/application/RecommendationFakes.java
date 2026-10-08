package com.energymonitor.recommendation.application;

import com.energymonitor.recommendation.api.RecommendationCreated;
import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.application.port.out.ConsumptionReaderPort;
import com.energymonitor.recommendation.application.port.out.HomeReaderPort;
import com.energymonitor.recommendation.application.port.out.RecommendationEventPort;
import com.energymonitor.recommendation.application.port.out.RecommendationPersistencePort;
import com.energymonitor.recommendation.domain.model.ConsumptionLimit;
import com.energymonitor.recommendation.domain.model.HourSample;
import com.energymonitor.recommendation.domain.model.MonitoredDevice;
import com.energymonitor.recommendation.domain.model.Recommendation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** In-memory ports for the recommendation use cases. */
final class RecommendationFakes {

    private RecommendationFakes() {
    }

    static final class Store implements RecommendationPersistencePort {
        final Map<String, Recommendation> rows = new LinkedHashMap<>();
        final Set<String> deleted = new HashSet<>();

        @Override
        public Recommendation save(Recommendation r) {
            rows.put(r.idRecommendation(), r);
            return r;
        }

        @Override
        public Optional<Recommendation> findActive(String id) {
            return deleted.contains(id) ? Optional.empty() : Optional.ofNullable(rows.get(id));
        }

        @Override
        public List<Recommendation> listActiveByHome(String homeId) {
            return rows.values().stream()
                    .filter(r -> r.homeId().equals(homeId) && !deleted.contains(r.idRecommendation()))
                    .sorted(Comparator.comparing(Recommendation::dateTime).reversed())
                    .toList();
        }

        @Override
        public boolean existsSince(String homeId, RecommendationType type, String deviceId, Instant since) {
            return rows.values().stream().anyMatch(r -> r.homeId().equals(homeId) && r.type() == type
                    && Objects.equals(r.deviceId(), deviceId) && !r.dateTime().isBefore(since));
        }

        @Override
        public void delete(String id) {
            deleted.add(id);
        }

        @Override
        public int deleteRead(String homeId) {
            int n = 0;
            for (Recommendation r : listActiveByHome(homeId)) {
                if (r.isRead()) {
                    deleted.add(r.idRecommendation());
                    n++;
                }
            }
            return n;
        }
    }

    static final class Homes implements HomeReaderPort {
        final Map<String, String> deviceHome = new HashMap<>();
        final Map<String, List<MonitoredDevice>> devices = new HashMap<>();
        final Map<String, Set<String>> members = new HashMap<>();
        final Map<String, ConsumptionLimit> limits = new HashMap<>();

        void device(String homeId, String deviceId, boolean alwaysOn) {
            deviceHome.put(deviceId, homeId);
            devices.computeIfAbsent(homeId, k -> new ArrayList<>()).add(new MonitoredDevice(deviceId, alwaysOn));
        }

        @Override
        public Optional<String> homeOfDevice(String deviceId) {
            return Optional.ofNullable(deviceHome.get(deviceId));
        }

        @Override
        public List<MonitoredDevice> devicesOf(String homeId) {
            return devices.getOrDefault(homeId, List.of());
        }

        @Override
        public Optional<ConsumptionLimit> limitOf(String homeId) {
            return Optional.ofNullable(limits.get(homeId));
        }

        @Override
        public boolean isMember(String userId, String homeId) {
            return members.getOrDefault(homeId, Set.of()).contains(userId);
        }

        @Override
        public Optional<String> deviceName(String deviceId) {
            return Optional.of("Device " + deviceId);
        }
    }

    static final class Consumption implements ConsumptionReaderPort {
        final List<HourSample> samples = new ArrayList<>();

        @Override
        public List<HourSample> hourly(Collection<String> deviceIds, Instant from, Instant to) {
            return samples.stream().filter(s -> deviceIds.contains(s.deviceId())
                    && !s.hourStart().isBefore(from) && s.hourStart().isBefore(to)).toList();
        }

        @Override
        public List<String> devicesReportingSince(Instant since) {
            return samples.stream().filter(s -> !s.hourStart().isBefore(since))
                    .map(HourSample::deviceId).distinct().toList();
        }
    }

    static final class Events implements RecommendationEventPort {
        final List<RecommendationCreated> published = new ArrayList<>();

        @Override
        public void publish(RecommendationCreated event) {
            published.add(event);
        }
    }
}
