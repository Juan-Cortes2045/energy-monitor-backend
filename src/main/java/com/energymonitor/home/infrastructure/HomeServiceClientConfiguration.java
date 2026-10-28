package com.energymonitor.home.infrastructure;

import com.energymonitor.home.adapter.out.remote.HomeServiceClient;
import com.energymonitor.home.adapter.out.remote.RemoteHomeApi;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connects this backend to the home service: the client of its internal API, with its address,
 * timeouts and the shared token, and {@link RemoteHomeApi} as the {@code HomeApi} the other modules
 * read homes with, cached.
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

    @Bean
    public RemoteHomeApi homeApi(HomeServiceClient client, HomeServiceProperties properties) {
        return new RemoteHomeApi(client, properties.ttl(), properties.staleTtl(), Ticker.systemTicker());
    }
}
