package com.energymonitor.measurement.adapter.in.web.dto;

import com.energymonitor.measurement.application.result.HomeConsumptionHistoryResult;
import java.time.Instant;
import java.util.List;

/**
 * {@code GET /api/v1/homes/{homeId}/consumption/history}. Energies in kWh.
 */
public record HomeConsumptionHistoryResponse(String period, Instant from, Instant to, List<Bucket> buckets,
                                             double previousTotal) {

    public record Bucket(String key, Instant start, List<DeviceEnergy> devices) {
    }

    public record DeviceEnergy(String deviceId, double energy) {
    }

    public static HomeConsumptionHistoryResponse from(HomeConsumptionHistoryResult r) {
        return new HomeConsumptionHistoryResponse(r.period(), r.from(), r.to(), r.buckets().stream()
                .map(b -> new Bucket(b.key(), b.start(), b.devices().stream()
                        .map(d -> new DeviceEnergy(d.deviceId(), d.energy()))
                        .toList()))
                .toList(), r.previousTotal());
    }
}
