package com.energymonitor.home.infrastructure;

import com.energymonitor.home.adapter.out.config.HomeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Home Management module.
 *
 * <p>Registers {@link HomeProperties} as a bean so that {@code @ConfigurationProperties}
 * binding is processed.
 */
@Configuration
@EnableConfigurationProperties(HomeProperties.class)
public class HomeServiceConfiguration {
}
