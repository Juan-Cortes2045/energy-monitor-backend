package com.energymonitor.alert.adapter.in.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.energymonitor.alert.application.command.RegisterThresholdAlertCommand;
import com.energymonitor.alert.application.port.in.RegisterThresholdAlert;
import com.energymonitor.alert.application.result.AlertResult;
import com.energymonitor.measurement.api.MeasurementRecorded;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MeasurementRecordedListenerTest {

    private static final Instant DATE_TIME = Instant.parse("2026-01-15T10:30:00Z");

    private static final class CapturingRegisterThresholdAlert implements RegisterThresholdAlert {
        private final List<RegisterThresholdAlertCommand> commands = new ArrayList<>();

        @Override
        public Optional<AlertResult> register(RegisterThresholdAlertCommand command) {
            commands.add(command);
            return Optional.empty();
        }
    }

    @Test
    void forwardsTheEventToTheUseCase() {
        var useCase = new CapturingRegisterThresholdAlert();
        var limitChecks = new ArrayList<String>();
        var linkChecks = new ArrayList<String>();
        var listener = new MeasurementRecordedListener(useCase,
                (deviceId, dateTime) -> limitChecks.add(deviceId),
                (deviceId, dateTime) -> linkChecks.add(deviceId));
        var event = new MeasurementRecorded("mea0000001", "dev0000001", DATE_TIME,
                120.5, 2.5, 300.0, 1520.75);

        listener.on(event);

        assertEquals(1, useCase.commands.size());
        var command = useCase.commands.getFirst();
        assertEquals("dev0000001", command.deviceId());
        assertEquals("mea0000001", command.measurementId());
        assertEquals(DATE_TIME, command.dateTime());
        assertEquals(300.0, command.activePower());
        assertEquals(List.of("dev0000001"), limitChecks);
        assertEquals(List.of("dev0000001"), linkChecks);
    }
}
