package com.energymonitor.alert.adapter.in.event;

import com.energymonitor.alert.application.command.RegisterThresholdAlertCommand;
import com.energymonitor.alert.application.port.in.RegisterDeviceLinkedAlert;
import com.energymonitor.alert.application.port.in.RegisterLimitAlert;
import com.energymonitor.alert.application.port.in.RegisterThresholdAlert;
import com.energymonitor.measurement.api.MeasurementRecorded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The class diagram's {@code AlertObserver}: subscribes to the measurement module's
 * {@code MeasurementRecorded} event and hands every reading to the alert use cases: the power
 * level of the reading, the home's consumption limit, and the first reading of a newly linked
 * device.
 *
 * <p>Deliberately thin: every decision lives in the application layer. A failure of the limit or
 * link checks is logged and never stops the threshold alert of the same reading.
 */
@Component
public class MeasurementRecordedListener {

    private static final Logger log = LoggerFactory.getLogger(MeasurementRecordedListener.class);

    private final RegisterThresholdAlert registerThresholdAlert;
    private final RegisterLimitAlert registerLimitAlert;
    private final RegisterDeviceLinkedAlert registerDeviceLinkedAlert;

    public MeasurementRecordedListener(RegisterThresholdAlert registerThresholdAlert,
                                       RegisterLimitAlert registerLimitAlert,
                                       RegisterDeviceLinkedAlert registerDeviceLinkedAlert) {
        this.registerThresholdAlert = registerThresholdAlert;
        this.registerLimitAlert = registerLimitAlert;
        this.registerDeviceLinkedAlert = registerDeviceLinkedAlert;
    }

    @EventListener
    public void on(MeasurementRecorded event) {
        try {
            registerDeviceLinkedAlert.onMeasurement(event.deviceId(), event.dateTime());
        } catch (RuntimeException e) {
            log.warn("Linked-device alert of {} could not be evaluated: {}", event.deviceId(), e.getMessage());
        }
        registerThresholdAlert.register(new RegisterThresholdAlertCommand(
                event.deviceId(), event.idMeasurement(), event.dateTime(), event.activePower()));
        try {
            registerLimitAlert.evaluate(event.deviceId(), event.dateTime());
        } catch (RuntimeException e) {
            log.warn("Consumption limit of {} could not be evaluated: {}", event.deviceId(), e.getMessage());
        }
    }
}
