package com.energymonitor.device.adapter.out.remote;

import com.energymonitor.device.adapter.out.remote.DeviceServiceClient.RemoteDevice;
import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.device.api.DeviceServiceUnavailableException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link DeviceApi} answered by the device service, for the modules that read devices: the
 * recommendations, which read the devices of a home and their names.
 *
 * <p>Answers are reused for a short while, a device that does not exist included, so a module
 * running every hour costs one request per {@code ttl} and home. A device
 * unlinked or renamed in the service shows here once its entry expires, or at once when
 * {@link #forget} is called for it.
 *
 * <p>The last answer is also kept for {@code staleTtl}: when the service does not answer, that is
 * what is served, so a short outage of the device service does not stop the recommendations of devices
 * already known. Only with nothing kept does a call throw {@link DeviceServiceUnavailableException}.
 *
 * <p>Connectivity and credentials are not read here: the device service keeps them from the broker
 * itself.
 */
public class RemoteDeviceApi implements DeviceApi, DeviceChanges {

    private final DeviceServiceClient client;
    private final Cache<String, Optional<RemoteDevice>> devices;
    private final Cache<String, Optional<RemoteDevice>> lastKnownDevices;
    private final Cache<String, List<RemoteDevice>> homes;
    private final Cache<String, List<RemoteDevice>> lastKnownHomes;

    /**
     * @param client   the device service
     * @param ttl      how long an answer is reused before asking again
     * @param staleTtl how long the last answer is kept to serve while the service does not answer
     * @param ticker   the clock of the caches
     */
    public RemoteDeviceApi(DeviceServiceClient client, Duration ttl, Duration staleTtl, Ticker ticker) {
        this.client = client;
        this.devices = cache(ttl, ticker);
        this.lastKnownDevices = cache(staleTtl, ticker);
        this.homes = cache(ttl, ticker);
        this.lastKnownHomes = cache(staleTtl, ticker);
    }

    private static <V> Cache<String, V> cache(Duration ttl, Ticker ticker) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).ticker(ticker).build();
    }

    @Override
    public Optional<DeviceDto> findByDeviceId(String deviceId) {
        return device(deviceId).map(RemoteDeviceApi::toDto);
    }

    @Override
    public Optional<String> getHomeId(String deviceId) {
        return device(deviceId).map(RemoteDevice::homeId);
    }

    @Override
    public List<DeviceDto> findByHome(String homeId) {
        if (homeId == null) {
            return List.of();
        }
        return fresh(homes, lastKnownHomes, homeId, client::devicesOfHome).stream()
                .map(RemoteDeviceApi::toDto)
                .toList();
    }

    /**
     * Drops what is known of a device and of a home, so the next call asks the service again.
     * Called when the device service announces a link or an unlink.
     *
     * @param deviceId the device that changed
     * @param homeId   the home it joined or left, may be null
     */
    @Override
    public void forget(String deviceId, String homeId) {
        if (deviceId != null) {
            devices.invalidate(deviceId);
        }
        if (homeId != null) {
            homes.invalidate(homeId);
        }
    }

    private Optional<RemoteDevice> device(String deviceId) {
        if (deviceId == null) {
            return Optional.empty();
        }
        return fresh(devices, lastKnownDevices, deviceId, client::device);
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
        } catch (DeviceServiceUnavailableException unavailable) {
            V stale = lastKnown.getIfPresent(key);
            if (stale != null) {
                return stale;
            }
            throw unavailable;
        }
    }

    private static DeviceDto toDto(RemoteDevice device) {
        return new DeviceDto(device.idDevice(), device.name(), device.applianceTypeId(), device.location(),
                device.description(), device.installationDate(), device.deviceCode());
    }
}
