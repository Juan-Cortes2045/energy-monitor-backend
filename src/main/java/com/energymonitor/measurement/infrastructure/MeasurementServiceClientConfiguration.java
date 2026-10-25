package com.energymonitor.measurement.infrastructure;

import com.energymonitor.measurement.adapter.out.remote.MeasurementServiceClient;
import com.energymonitor.measurement.adapter.out.remote.RemoteMeasurementApi;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connects this backend to the measurement service: the client of its internal API, with its
 * address, timeouts and the shared token, and {@link RemoteMeasurementApi} as the
 * {@code MeasurementApi} the alerts and the recommendations read with.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MeasurementServiceProperties.class)
public class MeasurementServiceClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementServiceClientConfiguration.class);

    @Bean
    public MeasurementServiceClient measurementServiceClient(MeasurementServiceProperties properties,
                                                             @Value("${internal-api.token:}") String token) {
        if (token == null || token.isBlank()) {
            LOG.warn("internal-api.token is empty: the measurement service will refuse every call, so "
                    + "no reading can be classified");
        }
        return MeasurementServiceClient.create(properties.url().toString(), token == null ? "" : token,
                properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    public RemoteMeasurementApi measurementApi(MeasurementServiceClient client,
                                               MeasurementServiceProperties properties) {
        return new RemoteMeasurementApi(client, properties.levelsTtl(), properties.levelsStaleTtl(),
                Ticker.systemTicker());
    }
}
