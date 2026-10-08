package com.energymonitor.notification.adapter.out.client;

import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.home.api.HomeApi;
import com.energymonitor.home.api.HomeDto;
import com.energymonitor.notification.application.port.out.HomeDirectoryPort;
import com.energymonitor.security.api.UserProfile;
import com.energymonitor.security.api.UserProfileQuery;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reads members, names and addresses through the public APIs of home, device and security.
 */
@Component
public class HomeDirectoryAdapter implements HomeDirectoryPort {

    private final HomeApi homeApi;
    private final DeviceApi deviceApi;
    private final UserProfileQuery profiles;

    public HomeDirectoryAdapter(HomeApi homeApi, DeviceApi deviceApi, UserProfileQuery profiles) {
        this.homeApi = homeApi;
        this.deviceApi = deviceApi;
        this.profiles = profiles;
    }

    @Override
    public List<String> memberIds(String homeId) {
        return homeApi.memberIds(homeId);
    }

    @Override
    public Map<String, Recipient> recipients(List<String> userIds) {
        return profiles.findByIds(userIds).values().stream()
                .collect(Collectors.toMap(UserProfile::userId,
                        p -> new Recipient(p.userId(), p.name(), p.email())));
    }

    @Override
    public Optional<String> homeName(String homeId) {
        return homeApi.findHome(homeId).map(HomeDto::name);
    }

    @Override
    public Optional<String> deviceName(String deviceId) {
        return deviceId == null ? Optional.empty() : deviceApi.findByDeviceId(deviceId).map(DeviceDto::name);
    }
}
