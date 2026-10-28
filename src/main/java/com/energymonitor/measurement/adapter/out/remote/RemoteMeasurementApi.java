package com.energymonitor.measurement.adapter.out.remote;

import com.energymonitor.measurement.adapter.out.remote.MeasurementServiceClient.RemoteLevel;
import com.energymonitor.measurement.api.ConsumptionLevelDto;
import com.energymonitor.measurement.api.HourlyEnergyDto;
import com.energymonitor.measurement.api.MeasurementApi;
import com.energymonitor.measurement.api.MeasurementServiceUnavailableException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link MeasurementApi} answered by the measurement service, for the
 * recommendations.
 *
 * <p>The consumption levels are a catalogue of four rows that rarely changes, and every reading is
 * classified with them, so the list is reused for {@code levelsTtl} and every reading is classified
 * here against it, by the same {@code [minLimit, maxLimit)} rule the service uses. The last list is
 * also kept for {@code levelsStaleTtl}, served while the service does not answer.
 *
 * <p>The energy and the reporting devices change with every reading, so they are always asked.
 */
public class RemoteMeasurementApi implements MeasurementApi {

    private static final String LEVELS = "levels";

    private final MeasurementServiceClient client;
    private final Cache<String, List<RemoteLevel>> levels;
    private final Cache<String, List<RemoteLevel>> lastKnownLevels;

    /**
     * @param client         the measurement service
     * @param levelsTtl      how long the level catalogue is reused before asking again
     * @param levelsStaleTtl how long the last catalogue is kept to serve while the service does
     *                       not answer
     * @param ticker         the clock of the caches
     */
    public RemoteMeasurementApi(MeasurementServiceClient client, Duration levelsTtl, Duration levelsStaleTtl,
                                Ticker ticker) {
        this.client = client;
        this.levels = Caffeine.newBuilder().expireAfterWrite(levelsTtl).ticker(ticker).build();
        this.lastKnownLevels = Caffeine.newBuilder().expireAfterWrite(levelsStaleTtl).ticker(ticker).build();
    }

    @Override
    public Optional<ConsumptionLevelDto> classify(double activePower) {
        return levels().stream()
                .filter(level -> activePower >= level.minLimit() && activePower < level.maxLimit())
                .findFirst()
                .map(level -> new ConsumptionLevelDto(level.idConsumptionLevel(), level.name(),
                        level.description(), level.minLimit(), level.maxLimit()));
    }

    @Override
    public List<HourlyEnergyDto> hourlyEnergy(Collection<String> deviceIds, Instant from, Instant to) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        return client.hourlyEnergy(deviceIds, from, to).stream()
                .map(h -> new HourlyEnergyDto(h.deviceId(), h.hourStart(), h.averagePower(), h.energy()))
                .toList();
    }

    @Override
    public List<String> devicesReportingSince(Instant since) {
        return client.reportingDevices(since);
    }

    /**
     * The cached catalogue, or a new one, which is also kept as the last known; when the service
     * does not answer, the last known one if there is any.
     */
    private List<RemoteLevel> levels() {
        List<RemoteLevel> cached = levels.getIfPresent(LEVELS);
        if (cached != null) {
            return cached;
        }
        try {
            List<RemoteLevel> loaded = client.levels();
            levels.put(LEVELS, loaded);
            lastKnownLevels.put(LEVELS, loaded);
            return loaded;
        } catch (MeasurementServiceUnavailableException unavailable) {
            List<RemoteLevel> stale = lastKnownLevels.getIfPresent(LEVELS);
            if (stale != null) {
                return stale;
            }
            throw unavailable;
        }
    }
}
