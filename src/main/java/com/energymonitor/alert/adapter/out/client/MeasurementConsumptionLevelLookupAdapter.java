package com.energymonitor.alert.adapter.out.client;

import com.energymonitor.alert.application.port.out.ConsumptionLevelLookupPort;
import com.energymonitor.measurement.api.MeasurementApi;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Classifies active power values through the measurement module's public API.
 *
 * <p>The only type this context imports from {@code measurement} is its
 * {@code measurement::api} named interface, exactly what the module declaration allows.
 */
@Component
public class MeasurementConsumptionLevelLookupAdapter implements ConsumptionLevelLookupPort {

    private final MeasurementApi measurementApi;

    public MeasurementConsumptionLevelLookupAdapter(MeasurementApi measurementApi) {
        this.measurementApi = measurementApi;
    }

    @Override
    public Optional<ConsumptionLevelView> classify(double activePower) {
        return measurementApi.classify(activePower)
                .map(level -> new ConsumptionLevelView(level.idConsumptionLevel(), level.name()));
    }
}
