package com.energymonitor.measurement.adapter.in.web.dto;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.result.HomeConsumptionSummaryResult;
import java.time.Instant;
import java.util.List;

/**
 * {@code GET /api/v1/homes/{homeId}/consumption/summary}. Powers in W, energies in kWh.
 */
public record HomeConsumptionSummaryResponse(Double currentPower, RiskConsumption level, double todayEnergy,
                                             double monthEnergy, Double dailyLimit, Double monthlyLimit,
                                             String limitPeriod, List<HourPower> lastHours, List<DeviceConsumption> devices) {

    public record HourPower(Instant hourStart, Double averagePower) {
    }

    public record DeviceConsumption(String deviceId, Double currentPower, double todayEnergy) {
    }

    public static HomeConsumptionSummaryResponse from(HomeConsumptionSummaryResult r) {
        return new HomeConsumptionSummaryResponse(r.currentPower(), r.level(), r.todayEnergy(), r.monthEnergy(),
                r.dailyLimit(), r.monthlyLimit(), r.limitPeriod(),
                r.lastHours().stream().map(h -> new HourPower(h.hourStart(), h.averagePower())).toList(),
                r.devices().stream()
                        .map(d -> new DeviceConsumption(d.deviceId(), d.currentPower(), d.todayEnergy()))
                        .toList());
    }
}
