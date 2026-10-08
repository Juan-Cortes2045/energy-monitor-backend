package com.energymonitor.measurement.application.result;

import java.time.Instant;
import java.util.List;

/**
 * Energy of each device of a home per bucket of a period.
 *
 * @param period  {@code day}, {@code week}, {@code month} or {@code year}
 * @param buckets in chronological order; {@code key} is the local hour ({@code "0"}..{@code "23"}),
 *                the local date ({@code yyyy-MM-dd}) or the local month ({@code yyyy-MM})
 * @param previousTotal energy of all devices in the previous period of the same kind (yesterday,
 *                the 7 days before, last month, last year), kWh
 */
public record HomeConsumptionHistoryResult(String period, Instant from, Instant to, List<Bucket> buckets,
                                           double previousTotal) {

    public record Bucket(String key, Instant start, List<DeviceEnergy> devices) {
    }

    /** @param energy kWh */
    public record DeviceEnergy(String deviceId, double energy) {
    }
}
