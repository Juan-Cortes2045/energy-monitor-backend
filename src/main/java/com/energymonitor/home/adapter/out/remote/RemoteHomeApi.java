package com.energymonitor.home.adapter.out.remote;

import com.energymonitor.home.adapter.out.remote.HomeServiceClient.RemoteAccess;
import com.energymonitor.home.adapter.out.remote.HomeServiceClient.RemoteHome;
import com.energymonitor.home.adapter.out.remote.HomeServiceClient.RemoteLimits;
import com.energymonitor.home.api.HomeApi;
import com.energymonitor.home.api.HomeDto;
import com.energymonitor.home.api.HomeServiceUnavailableException;
import com.energymonitor.home.api.HomeThresholdsDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link HomeApi} answered by the home service, for the modules that read homes: the alerts (the
 * limit of a home, whether a user may see the alerts of a device), the notifications (who the
 * members are, the name of the home) and the recommendations.
 *
 * <p>Answers are reused for a short while, a missing home included, so an alert evaluated on every
 * reading costs one request per {@code ttl}, not one per reading. A change in the home service
 * shows here once its entry expires.
 *
 * <p>For a home and its limits, the last answer is also kept for {@code staleTtl}: when the
 * service does not answer, that is what is served, so a short outage does not stop the alerts and
 * notifications of homes already known. Who may act on a home is never served stale: a removed
 * member must not keep access because the service is down, so with nothing fresh that call throws
 * {@link HomeServiceUnavailableException}, as does any call with nothing kept.
 */
public class RemoteHomeApi implements HomeApi {

    private final HomeServiceClient client;
    private final Cache<String, Optional<RemoteHome>> homes;
    private final Cache<String, Optional<RemoteHome>> lastKnownHomes;
    private final Cache<String, Optional<RemoteLimits>> limits;
    private final Cache<String, Optional<RemoteLimits>> lastKnownLimits;
    private final Cache<String, RemoteAccess> access;

    /**
     * @param client   the home service
     * @param ttl      how long an answer is reused before asking again
     * @param staleTtl how long the last home and limits are kept to serve while the service does
     *                 not answer
     * @param ticker   the clock of the caches
     */
    public RemoteHomeApi(HomeServiceClient client, Duration ttl, Duration staleTtl, Ticker ticker) {
        this.client = client;
        this.homes = cache(ttl, ticker);
        this.lastKnownHomes = cache(staleTtl, ticker);
        this.limits = cache(ttl, ticker);
        this.lastKnownLimits = cache(staleTtl, ticker);
        this.access = cache(ttl, ticker);
    }

    private static <V> Cache<String, V> cache(Duration ttl, Ticker ticker) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).ticker(ticker).build();
    }

    @Override
    public boolean homeExists(String idHome) {
        return home(idHome).isPresent();
    }

    @Override
    public boolean isMember(String idUser, String idHome) {
        return accessOf(idUser, idHome).member();
    }

    @Override
    public boolean isOwner(String idUser, String idHome) {
        return accessOf(idUser, idHome).owner();
    }

    @Override
    public List<String> memberIds(String idHome) {
        return home(idHome).map(RemoteHome::memberIds).orElse(List.of());
    }

    @Override
    public Optional<HomeDto> findHome(String idHome) {
        return home(idHome).map(home -> new HomeDto(home.homeId(), home.name(), home.homeTypeId(),
                home.address(), home.creationDate()));
    }

    /**
     * The limits and their period, which is all the alerts and recommendations read. The home
     * service does not expose the identifier of the thresholds row nor whether they are the
     * system defaults, so those come back as {@code null} and {@code false}.
     */
    @Override
    public Optional<HomeThresholdsDto> findThresholds(String idHome) {
        if (idHome == null) {
            return Optional.empty();
        }
        return fresh(limits, lastKnownLimits, idHome, client::limits)
                .map(found -> new HomeThresholdsDto(null, idHome, found.dailyLimit(),
                        found.monthlyLimit(), false, found.limitPeriod()));
    }

    private Optional<RemoteHome> home(String idHome) {
        if (idHome == null) {
            return Optional.empty();
        }
        return fresh(homes, lastKnownHomes, idHome, client::home);
    }

    private RemoteAccess accessOf(String idUser, String idHome) {
        if (idUser == null || idHome == null) {
            return new RemoteAccess(false, false, false);
        }
        // Not served stale: see the class comment.
        return access.get(idHome + "|" + idUser, key -> client.access(idHome, idUser));
    }

    /**
     * The cached answer, or a new one, which is also kept as the last known; when the service does
     * not answer, the last known one if there is any.
     */
    private static <V> V fresh(Cache<String, V> cache, Cache<String, V> lastKnown, String key,
                               Function<String, V> load) {
        V cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        try {
            V loaded = load.apply(key);
            cache.put(key, loaded);
            lastKnown.put(key, loaded);
            return loaded;
        } catch (HomeServiceUnavailableException unavailable) {
            V stale = lastKnown.getIfPresent(key);
            if (stale != null) {
                return stale;
            }
            throw unavailable;
        }
    }
}
