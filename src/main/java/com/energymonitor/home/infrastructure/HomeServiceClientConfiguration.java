package com.energymonitor.home.infrastructure;

import com.energymonitor.home.adapter.out.remote.HomeServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The client of the home service's internal API, with its address, timeouts and the shared token.
 *
 * <p>Nothing reads homes through it yet: {@code HomeApiImpl} still answers from this backend's
 * own tables. {@code RemoteHomeApi} takes its place when the home module is removed.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HomeServiceProperties.class)
public class HomeServiceClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HomeServiceClientConfiguration.class);

    @Bean
    public HomeServiceClient homeServiceClient(HomeServiceProperties properties,
                                               @Value("${internal-api.token:}") String token) {
        if (token == null || token.isBlank()) {
            LOG.warn("internal-api.token is empty: the home service will refuse every call");
        }
        return HomeServiceClient.create(properties.url().toString(), token == null ? "" : token,
                properties.connectTimeout(), properties.readTimeout());
    }
}
