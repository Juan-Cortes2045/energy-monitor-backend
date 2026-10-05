package com.energymonitor.measurement.infrastructure;

import com.energymonitor.measurement.api.ConsumptionLevelDto;
import com.energymonitor.measurement.api.MeasurementApi;
import com.energymonitor.measurement.api.MeasurementDto;
import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.port.out.ConsumptionLevelPersistencePort;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import com.energymonitor.measurement.domain.model.Measurement;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link MeasurementApi} for consumption by other modules.
 */
@Component
public class MeasurementApiImpl implements MeasurementApi {

    private final MeasurementPersistencePort measurementPort;
    private final ConsumptionLevelPersistencePort levelPort;

    public MeasurementApiImpl(MeasurementPersistencePort measurementPort,
                              ConsumptionLevelPersistencePort levelPort) {
        this.measurementPort = measurementPort;
        this.levelPort = levelPort;
    }

    @Override
    public Optional<MeasurementDto> findLatestByDevice(String deviceId) {
        return measurementPort.findLatestActiveByDevice(deviceId)
                .map(MeasurementApiImpl::toDto);
    }

    @Override
    public Optional<ConsumptionLevelDto> classify(double activePower) {
        return levelPort.findLevelFor(activePower)
                .map(MeasurementApiImpl::toDto);
    }

    @Override
    public Optional<ConsumptionLevelDto> findLevel(RiskConsumption name) {
        return levelPort.findActiveByName(name)
                .map(MeasurementApiImpl::toDto);
    }

    private static MeasurementDto toDto(Measurement measurement) {
        return new MeasurementDto(measurement.idMeasurement(), measurement.deviceId(),
                measurement.dateTime(), measurement.getVoltage(), measurement.getCurrent(),
                measurement.getActivePower(), measurement.getStoredEnergy());
    }

    private static ConsumptionLevelDto toDto(ConsumptionLevel level) {
        return new ConsumptionLevelDto(level.idConsumptionLevel(), level.name(),
                level.description(), level.minLimit(), level.maxLimit());
    }
}
