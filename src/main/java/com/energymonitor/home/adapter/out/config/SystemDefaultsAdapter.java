package com.energymonitor.home.adapter.out.config;

import com.energymonitor.home.application.port.out.SystemDefaultsPort;
import org.springframework.stereotype.Component;

/**
 * Reads system-wide default values from {@link HomeProperties}.
 */
@Component
public class SystemDefaultsAdapter implements SystemDefaultsPort {

    private final HomeProperties properties;

    public SystemDefaultsAdapter(HomeProperties properties) {
        this.properties = properties;
    }

    @Override
    public double defaultDailyLimit() {
        return properties.getDailyLimit();
    }

    @Override
    public double defaultMonthlyLimit() {
        return properties.getMonthlyLimit();
    }
}
