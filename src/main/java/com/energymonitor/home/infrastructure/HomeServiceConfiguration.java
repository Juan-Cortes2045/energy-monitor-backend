package com.energymonitor.home.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.energymonitor.home.adapter.out.config.HomeProperties;

/**
 * Configuration for the Home Management module.
 *
 * <p>Registers {@link HomeProperties} as a bean so that {@code @ConfigurationProperties}
 * binding is processed, and registers the application use cases as Spring beans.
 */
@Configuration
@EnableConfigurationProperties(HomeProperties.class)
@ComponentScan(
    basePackages = "com.energymonitor.home.application.usecase",
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ".*Service"))
public class HomeServiceConfiguration {}