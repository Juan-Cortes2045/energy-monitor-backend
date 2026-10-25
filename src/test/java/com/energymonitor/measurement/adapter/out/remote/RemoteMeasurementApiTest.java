package com.energymonitor.measurement.adapter.out.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.energymonitor.measurement.adapter.out.remote.MeasurementServiceClient.RemoteHourlyEnergy;
import com.energymonitor.measurement.adapter.out.remote.MeasurementServiceClient.RemoteLevel;
import com.energymonitor.measurement.api.ConsumptionLevelDto;
import com.energymonitor.measurement.api.HourlyEnergyDto;
import com.energymonitor.measurement.api.MeasurementServiceUnavailableException;
import com.energymonitor.measurement.api.RiskConsumption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the cached {@code MeasurementApi} asks the measurement service, when it asks again, and
 * how it classifies a reading. The clock of the caches is moved by hand.
 */
@DisplayName("Measurement API answered by the measurement service")
class RemoteMeasurementApiTest {

    private static final List<RemoteLevel> LEVELS = List.of(
            new RemoteLevel("lev0000001", RiskConsumption.LOW, "Bajo", 0.0, 500.0),
            new RemoteLevel("lev0000002", RiskConsumption.MEDIUM, "Medio", 500.0, 1500.0),
            new RemoteLevel("lev0000003", RiskConsumption.HIGH, "Alto", 1500.0, 3000.0));
    private static final Instant FROM = Instant.parse("2026-10-24T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-24T23:59:59Z");

    private final MeasurementServiceClient client = mock(MeasurementServiceClient.class);
    private final AtomicLong nanos = new AtomicLong();
    private final RemoteMeasurementApi api = new RemoteMeasurementApi(client, Duration.ofMinutes(10),
            Duration.ofHours(24), nanos::get);

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    @DisplayName("classifies with the [minLimit, maxLimit) ranges, from one cached request")
    void classifies() {
        given(client.levels()).willReturn(LEVELS);

        assertThat(api.classify(0.0)).map(ConsumptionLevelDto::name).contains(RiskConsumption.LOW);
        assertThat(api.classify(499.9)).map(ConsumptionLevelDto::name).contains(RiskConsumption.LOW);
        assertThat(api.classify(500.0)).map(ConsumptionLevelDto::name).contains(RiskConsumption.MEDIUM);
        assertThat(api.classify(1500.0)).map(ConsumptionLevelDto::idConsumptionLevel).contains("lev0000003");
        assertThat(api.classify(3000.0)).as("no level covers it").isEmpty();

        verify(client, times(1)).levels();
    }

    @Test
    @DisplayName("asks the levels again once they expire, and serves the last ones while the service is down")
    void levelsExpireAndFallBack() {
        given(client.levels()).willReturn(LEVELS)
                .willThrow(new MeasurementServiceUnavailableException("down", null));

        api.classify(100.0);
        advance(Duration.ofMinutes(11));

        assertThat(api.classify(100.0)).isPresent();
        verify(client, times(2)).levels();
    }

    @Test
    @DisplayName("with no levels kept, an unavailable service fails the classification")
    void failsWithNothingKept() {
        given(client.levels()).willThrow(new MeasurementServiceUnavailableException("down", null));

        assertThatThrownBy(() -> api.classify(100.0)).isInstanceOf(MeasurementServiceUnavailableException.class);
    }

    @Test
    @DisplayName("the energy is always asked, and no device asks nothing")
    void hourlyEnergy() {
        given(client.hourlyEnergy(List.of("DEV0000001"), FROM, TO)).willReturn(List.of(
                new RemoteHourlyEnergy("DEV0000001", Instant.parse("2026-10-24T10:00:00Z"), 200.0, 0.2)));

        assertThat(api.hourlyEnergy(List.of("DEV0000001"), FROM, TO)).containsExactly(
                new HourlyEnergyDto("DEV0000001", Instant.parse("2026-10-24T10:00:00Z"), 200.0, 0.2));
        api.hourlyEnergy(List.of("DEV0000001"), FROM, TO);
        verify(client, times(2)).hourlyEnergy(List.of("DEV0000001"), FROM, TO);
    }

    @Test
    @DisplayName("no device asked for energy is no request")
    void noDevice() {
        assertThat(api.hourlyEnergy(List.of(), FROM, TO)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("the reporting devices come from the service")
    void reportingDevices() {
        given(client.reportingDevices(FROM)).willReturn(List.of("DEV0000001"));

        assertThat(api.devicesReportingSince(FROM)).containsExactly("DEV0000001");
    }
}
