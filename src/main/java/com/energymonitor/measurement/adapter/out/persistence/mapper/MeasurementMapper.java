package com.energymonitor.measurement.adapter.out.persistence.mapper;

import com.energymonitor.measurement.adapter.out.persistence.entity.MeasurementEntity;
import com.energymonitor.measurement.adapter.out.persistence.support.Instants;
import com.energymonitor.measurement.domain.model.Measurement;
import org.springframework.stereotype.Component;

/**
 * Converts {@link Measurement} to and from {@link MeasurementEntity}.
 */
@Component
public class MeasurementMapper {

    public MeasurementEntity toEntity(Measurement measurement) {
        return new MeasurementEntity(measurement.idMeasurement(), measurement.deviceId(),
                Instants.truncate(measurement.dateTime()), measurement.getVoltage(),
                measurement.getCurrent(), measurement.getActivePower(), measurement.getStoredEnergy());
    }

    public void applyTo(MeasurementEntity entity, Measurement measurement) {
        entity.setDeviceId(measurement.deviceId());
        entity.setDateTime(Instants.truncate(measurement.dateTime()));
        entity.setVoltage(measurement.getVoltage());
        entity.setCurrent(measurement.getCurrent());
        entity.setActivePower(measurement.getActivePower());
        entity.setStoredEnergy(measurement.getStoredEnergy());
    }

    public Measurement toDomain(MeasurementEntity entity) {
        return new Measurement(entity.getIdMeasurement(), entity.getDeviceId(),
                entity.getDateTime(), entity.getVoltage(), entity.getCurrent(),
                entity.getActivePower(), entity.getStoredEnergy());
    }
}
