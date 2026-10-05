package com.energymonitor.alert.adapter.in.event;

import com.energymonitor.alert.application.command.RegisterThresholdAlertCommand;
import com.energymonitor.alert.application.port.in.RegisterThresholdAlert;
import com.energymonitor.measurement.api.MeasurementRecorded;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The class diagram's {@code AlertObserver}: subscribes to the measurement module's
 * {@code MeasurementRecorded} event and hands the reading to the threshold-alert use case.
 *
 * <p>Deliberately thin: every decision (whether the level warrants an alert, which home
 * the device belongs to, what message key to use) lives in the application layer.
 */
@Component
public class MeasurementRecordedListener {

    private final RegisterThresholdAlert registerThresholdAlert;

    public MeasurementRecordedListener(RegisterThresholdAlert registerThresholdAlert) {
        this.registerThresholdAlert = registerThresholdAlert;
    }

    @EventListener
    public void on(MeasurementRecorded event) {
        registerThresholdAlert.register(new RegisterThresholdAlertCommand(
                event.deviceId(), event.idMeasurement(), event.dateTime(), event.activePower()));
    }
}
