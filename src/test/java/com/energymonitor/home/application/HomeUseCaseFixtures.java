package com.energymonitor.home.application;

import com.energymonitor.home.application.port.out.AccessCodeGeneratorPort;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.HomeTypePersistencePort;
import com.energymonitor.home.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.home.application.port.out.SystemDefaultsPort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.HomeType;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory fakes of the output ports, for application-layer tests only.
 *
 * <p>They replace the {@code @Component} persistence adapters so the use cases can be
 * exercised without a database.
 */
public final class HomeUseCaseFixtures {

    public static final Instant NOW = Instant.parse("2026-01-15T10:30:00Z");
    public static final double DEFAULT_DAILY_LIMIT = 10.0;
    public static final double DEFAULT_MONTHLY_LIMIT = 300.0;

    private HomeUseCaseFixtures() {
    }

    /** A clock fixed at {@link #NOW}. */
    public static Clock clock() {
        return Clock.fixed(NOW, ZoneId.of("UTC"));
    }

    public static class FakeHomePersistencePort implements HomePersistencePort {

        private final Map<String, Home> byId = new LinkedHashMap<>();
        private final Map<String, Home> byAccessCode = new LinkedHashMap<>();

        @Override
        public Home save(Home home) {
            byId.put(home.idHome(), home);
            byAccessCode.put(home.accessCode(), home);
            return home;
        }

        @Override
        public Optional<Home> findActive(String idHome) {
            return Optional.ofNullable(byId.get(idHome));
        }

        @Override
        public Optional<Home> findActiveByAccessCode(String accessCode) {
            return Optional.ofNullable(byAccessCode.get(accessCode));
        }

        @Override
        public boolean existsActiveByAccessCode(String accessCode) {
            return byAccessCode.containsKey(accessCode);
        }

        @Override
        public List<Home> findActiveByIds(Collection<String> ids) {
            return ids.stream()
                    .map(byId::get)
                    .filter(h -> h != null)
                    .toList();
        }

        public void seed(Home home) {
            byId.put(home.idHome(), home);
            byAccessCode.put(home.accessCode(), home);
        }
    }

    public static class FakeHomeTypePersistencePort implements HomeTypePersistencePort {

        private final Map<String, HomeType> byId = new LinkedHashMap<>();

        @Override
        public Optional<HomeType> findActive(String idHomeType) {
            return Optional.ofNullable(byId.get(idHomeType));
        }

        @Override
        public List<HomeType> findAllActive() {
            return new ArrayList<>(byId.values());
        }

        public void seed(HomeType type) {
            byId.put(type.idHomeType(), type);
        }
    }

    public static class FakeHomeThresholdsPersistencePort implements HomeThresholdsPersistencePort {

        private final Map<String, HomeThresholds> byId = new LinkedHashMap<>();
        private final Map<String, HomeThresholds> byHomeId = new LinkedHashMap<>();

        @Override
        public HomeThresholds save(HomeThresholds thresholds) {
            byId.put(thresholds.idThreshold(), thresholds);
            byHomeId.put(thresholds.homeId(), thresholds);
            return thresholds;
        }

        @Override
        public Optional<HomeThresholds> findActiveByHomeId(String homeId) {
            return Optional.ofNullable(byHomeId.get(homeId));
        }
    }

    public static class FakeUserHomePersistencePort implements UserHomePersistencePort {

        private final List<UserHome> memberships = new ArrayList<>();

        @Override
        public UserHome save(UserHome membership) {
            // Reactivation strategy: remove any existing (including soft-deleted) and add new
            memberships.removeIf(existing ->
                    existing.userId().equals(membership.userId()) && existing.homeId().equals(membership.homeId()));
            memberships.add(membership);
            return membership;
        }

        @Override
        public void remove(String userId, String homeId) {
            memberships.removeIf(existing ->
                    existing.userId().equals(userId) && existing.homeId().equals(homeId));
        }

        @Override
        public Optional<UserHome> findActive(String userId, String homeId) {
            return memberships.stream()
                    .filter(m -> m.userId().equals(userId) && m.homeId().equals(homeId))
                    .findFirst();
        }

        @Override
        public List<UserHome> listActiveByUser(String userId) {
            return memberships.stream()
                    .filter(m -> m.userId().equals(userId))
                    .toList();
        }

        @Override
        public List<UserHome> listActiveByHomeId(String homeId) {
            return memberships.stream()
                    .filter(m -> m.homeId().equals(homeId))
                    .toList();
        }

        @Override
        public long countActiveOwnersByHomeId(String homeId) {
            return memberships.stream()
                    .filter(m -> m.homeId().equals(homeId) && m.isOwner())
                    .count();
        }

        public List<UserHome> all() {
            return new ArrayList<>(memberships);
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

    public static class FakeAccessCodeGeneratorPort implements AccessCodeGeneratorPort {

        private int sequence;

        @Override
        public String generate() {
            sequence++;
            return "ac" + String.format("%06d", sequence);
        }
    }

    public static class FakeSystemDefaultsPort implements SystemDefaultsPort {

        @Override
        public double defaultDailyLimit() {
            return DEFAULT_DAILY_LIMIT;
        }

        @Override
        public double defaultMonthlyLimit() {
            return DEFAULT_MONTHLY_LIMIT;
        }
    }
}
