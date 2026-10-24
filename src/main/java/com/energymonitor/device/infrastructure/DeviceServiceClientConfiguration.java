package com.energymonitor.device.infrastructure;

import com.energymonitor.device.adapter.out.remote.DeviceServiceClient;
import com.energymonitor.device.adapter.out.remote.RemoteDeviceApi;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connects this backend to the device service: the client of its internal API, with its address,
 * timeouts and the shared token, and {@link RemoteDeviceApi} as the {@code DeviceApi} the other
 * modules read devices with, cached.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DeviceServiceProperties.class)
public class DeviceServiceClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceServiceClientConfiguration.class);

    @Bean
    public DeviceServiceClient deviceServiceClient(DeviceServiceProperties properties,
                                                   @Value("${internal-api.token:}") String token) {
        if (token == null || token.isBlank()) {
            LOG.warn("internal-api.token is empty: the device service will refuse every call, so no "
                    + "reading can be matched to its device");
        }
        return DeviceServiceClient.create(properties.url().toString(), token == null ? "" : token,
                properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    public RemoteDeviceApi deviceApi(DeviceServiceClient client, DeviceServiceProperties properties) {
        return new RemoteDeviceApi(client, properties.ttl(), properties.staleTtl(), Ticker.systemTicker());
    }
}
