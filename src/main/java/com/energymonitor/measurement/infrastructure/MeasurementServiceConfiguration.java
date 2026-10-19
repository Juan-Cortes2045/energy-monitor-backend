package com.energymonitor.measurement.infrastructure;

import com.energymonitor.measurement.application.port.out.ConsumptionLevelPersistencePort;
import com.energymonitor.measurement.application.port.out.HomeAccessPort;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.application.usecase.HomeConsumptionUseCase;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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
public class MeasurementServiceConfiguration {

    /**
     * A reading older than the device offline threshold no longer counts as current power.
     */
    @Bean
    public HomeConsumptionUseCase homeConsumptionUseCase(
            MeasurementPersistencePort measurements,
            ConsumptionLevelPersistencePort levels,
            HomeAccessPort homes,
            Clock clock,
            @Value("${device.connectivity.offline-after:PT150S}") Duration freshness) {
        return new HomeConsumptionUseCase(measurements, levels, homes, clock, freshness);
    }
}
