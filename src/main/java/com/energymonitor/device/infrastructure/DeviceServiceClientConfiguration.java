package com.energymonitor.device.infrastructure;

import com.energymonitor.device.adapter.out.remote.DeviceServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The client of the device service's internal API, with its address, timeouts and the shared
 * token.
 *
 * <p>Nothing reads devices through it yet: {@code DeviceApiImpl} still answers from this backend's
 * own tables. {@code RemoteDeviceApi} takes its place when the device module is removed.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DeviceServiceProperties.class)
public class DeviceServiceClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceServiceClientConfiguration.class);

    @Bean
    public DeviceServiceClient deviceServiceClient(DeviceServiceProperties properties,
                                                   @Value("${internal-api.token:}") String token) {
        if (token == null || token.isBlank()) {
            LOG.warn("internal-api.token is empty: the device service will refuse every call");
        }
        return DeviceServiceClient.create(properties.url().toString(), token == null ? "" : token,
                properties.connectTimeout(), properties.readTimeout());
    }
}
