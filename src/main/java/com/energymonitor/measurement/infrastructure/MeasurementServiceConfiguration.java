package com.energymonitor.measurement.infrastructure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Configuration for the Monitoring and Measurements module.
 *
 * <p>Registers the application use cases as Spring beans without annotating them, so the
 * application layer stays framework-free.
 */
@Configuration
@ComponentScan(
    basePackages = "com.energymonitor.measurement.application.usecase",
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ".*Service"))
public class MeasurementServiceConfiguration {}
