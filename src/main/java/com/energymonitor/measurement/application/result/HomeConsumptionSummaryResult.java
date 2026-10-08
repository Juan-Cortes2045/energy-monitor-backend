package com.energymonitor.measurement.application.result;

import com.energymonitor.measurement.api.RiskConsumption;
import java.time.Instant;
import java.util.List;

/**
 * Live view of a home.
 *
 * @param currentPower total active power of the devices reporting right now, W; null when none is
 * @param level        consumption level of {@code currentPower}; null when unknown
 * @param todayEnergy  energy consumed since local midnight, kWh
 * @param monthEnergy  energy consumed since the first day of the local month, kWh
 * @param dailyLimit   configured daily limit, kWh; null when the home has none
 * @param monthlyLimit configured monthly limit, kWh; null when the home has none
 * @param limitPeriod  {@code DAILY} or {@code MONTHLY}: which limit the owner set (the other is derived)
 * @param lastHours    average total power of each of the last 24 hours, oldest first
 * @param devices      the same figures per device
 */
public record HomeConsumptionSummaryResult(Double currentPower, RiskConsumption level, double todayEnergy,
                                           double monthEnergy, Double dailyLimit, Double monthlyLimit,
                                           String limitPeriod, List<HourPower> lastHours, List<DeviceConsumption> devices) {

    /** @param averagePower W, null when no device reported in that hour */
    public record HourPower(Instant hourStart, Double averagePower) {
    }

    /** @param currentPower W, null when the device is not reporting */
    public record DeviceConsumption(String deviceId, Double currentPower, double todayEnergy) {
    }
}
