package com.energymonitor.alert.adapter.out.client;

import com.energymonitor.alert.application.port.out.ConsumptionLimitPort;
import com.energymonitor.home.api.HomeApi;
import com.energymonitor.home.api.LimitPeriod;
import com.energymonitor.measurement.api.HourlyEnergyDto;
import com.energymonitor.measurement.api.MeasurementApi;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads the home's limit through {@code home::api} and its energy through {@code measurement::api},
 * the same hourly energy the consumption screens show.
 */
@Component
public class ConsumptionLimitAdapter implements ConsumptionLimitPort {

    private final HomeApi homeApi;
    private final MeasurementApi measurementApi;
    private final ZoneId zone;

    public ConsumptionLimitAdapter(HomeApi homeApi, MeasurementApi measurementApi,
                                   @Value("${alert.limit.zone:America/Bogota}") String zone) {
        this.homeApi = homeApi;
        this.measurementApi = measurementApi;
        this.zone = ZoneId.of(zone);
    }

    @Override
    public Optional<Limit> limitOf(String homeId) {
        return homeApi.findThresholds(homeId).map(t -> t.limitPeriod() == LimitPeriod.MONTHLY
                ? new Limit(true, t.monthlyLimit())
                : new Limit(false, t.dailyLimit()));
    }

    @Override
    public double energy(Collection<String> deviceIds, Instant from, Instant to) {
        if (deviceIds.isEmpty()) {
            return 0;
        }
        return measurementApi.hourlyEnergy(deviceIds, from, to).stream()
                .mapToDouble(HourlyEnergyDto::energy)
                .sum();
    }

    @Override
    public ZoneId zone() {
        return zone;
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}
