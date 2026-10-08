package com.energymonitor.device.infrastructure;

import com.energymonitor.device.application.port.out.DeviceEventPort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.application.port.out.DeviceStatusLogPersistencePort;
import com.energymonitor.device.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.device.application.usecase.BrokerAccessService;
import com.energymonitor.device.application.usecase.DeviceConnectivityService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Device module configuration.
 */
@Configuration
@EnableScheduling
public class DeviceServiceConfiguration {

    @Bean
    public DeviceConnectivityService deviceConnectivityService(
            DeviceStatusLogPersistencePort statusLogs,
            IdentifierGeneratorPort identifiers,
            DeviceEventPort events,
            Clock clock,
            @Value("${device.connectivity.offline-after:PT150S}") Duration offlineAfter) {
        return new DeviceConnectivityService(statusLogs, identifiers, events, clock, offlineAfter);
    }

    /**
     * The backend's own broker credentials are the ones it connects with
     * ({@code mqtt.username} / {@code mqtt.password}), so they are configured once.
     */
    @Bean
    public BrokerAccessService brokerAccessService(
            DevicePersistencePort devices,
            @Value("${mqtt.username:}") String backendUsername,
            @Value("${mqtt.password:}") String backendPassword) {
        return new BrokerAccessService(devices, backendUsername, backendPassword);
    }
}
