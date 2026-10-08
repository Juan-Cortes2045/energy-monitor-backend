package com.energymonitor.recommendation.adapter.out.client;

import com.energymonitor.recommendation.application.port.out.ConsumptionReaderPort;
import com.energymonitor.recommendation.domain.model.HourSample;
import com.energymonitor.measurement.api.MeasurementApi;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reads hourly consumption through {@code measurement::api}.
 */
@Component
public class MeasurementConsumptionReaderAdapter implements ConsumptionReaderPort {

    private final MeasurementApi measurementApi;

    public MeasurementConsumptionReaderAdapter(MeasurementApi measurementApi) {
        this.measurementApi = measurementApi;
    }

    @Override
    public List<HourSample> hourly(Collection<String> deviceIds, Instant from, Instant to) {
        return measurementApi.hourlyEnergy(deviceIds, from, to).stream()
                .map(h -> new HourSample(h.deviceId(), h.hourStart(), h.averagePower(), h.energy()))
                .toList();
    }

    @Override
    public List<String> devicesReportingSince(Instant since) {
        return measurementApi.devicesReportingSince(since);
    }
}
