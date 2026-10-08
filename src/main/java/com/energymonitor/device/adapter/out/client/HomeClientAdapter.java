package com.energymonitor.device.adapter.out.client;

import com.energymonitor.device.application.port.out.HomeLookupPort;
import com.energymonitor.home.api.HomeApi;
import org.springframework.stereotype.Component;

@Component
public class HomeClientAdapter implements HomeLookupPort {

    private final HomeApi homeApi;

    public HomeClientAdapter(HomeApi homeApi) {
        this.homeApi = homeApi;
    }

    @Override
    public boolean homeExists(String homeId) {
        return homeId != null && homeApi.homeExists(homeId);
    }

    @Override
    public boolean isMember(String userId, String homeId) {
        if (userId == null || homeId == null) {
            return false;
        }
        return homeApi.isMember(userId, homeId);
    }

    @Override
    public boolean isOwner(String userId, String homeId) {
        if (userId == null || homeId == null) {
            return false;
        }
        return homeApi.isOwner(userId, homeId);
    }
}
