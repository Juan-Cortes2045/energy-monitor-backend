package com.energymonitor.measurement.adapter.out.persistence;

import com.energymonitor.measurement.adapter.out.persistence.entity.MeasurementEntity;
import com.energymonitor.measurement.adapter.out.persistence.mapper.MeasurementMapper;
import com.energymonitor.measurement.adapter.out.persistence.repository.MeasurementRepository;
import com.energymonitor.measurement.application.port.out.HourlyEnergyRow;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@link Measurement}.
 */
@Component
public class MeasurementPersistenceAdapter implements MeasurementPersistencePort {

    private final MeasurementRepository repository;
    private final MeasurementMapper mapper;

    public MeasurementPersistenceAdapter(MeasurementRepository repository, MeasurementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Measurement save(Measurement measurement) {
        MeasurementEntity entity = repository.findById(measurement.idMeasurement())
                .map(existing -> {
                    mapper.applyTo(existing, measurement);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(measurement));
        repository.save(entity);
        return measurement;
    }

    @Override
    public Optional<Measurement> findActive(String idMeasurement) {
        return repository.findById(idMeasurement)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<Measurement> listActiveByDevice(String deviceId, Instant from, Instant to) {
        return repository
                .findByDeviceIdAndDeletedAtIsNullAndDateTimeBetweenOrderByDateTimeAsc(deviceId, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Measurement> findLatestActiveByDevice(String deviceId) {
        return repository.findFirstByDeviceIdAndDeletedAtIsNullOrderByDateTimeDesc(deviceId)
                .map(mapper::toDomain);
    }

    @Override
    public List<HourlyEnergyRow> aggregateHourly(Collection<String> deviceIds, Instant from, Instant to) {
        if (deviceIds.isEmpty()) {
            return List.of();
        }
        return repository.aggregateHourly(deviceIds, from, to).stream()
                .map(row -> new HourlyEnergyRow(
                        (String) row[0],
                        Instant.ofEpochSecond(((Number) row[1]).longValue() * 3600),
                        ((Number) row[2]).doubleValue(),
                        ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).doubleValue()))
                .toList();
    }

    @Override
    public Optional<Measurement> findLatestActiveByDeviceBefore(String deviceId, Instant before) {
        return repository.findFirstByDeviceIdAndDeletedAtIsNullAndDateTimeBeforeOrderByDateTimeDesc(deviceId, before)
                .map(mapper::toDomain);
    }
}
