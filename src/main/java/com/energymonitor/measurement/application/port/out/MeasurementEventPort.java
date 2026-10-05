package com.energymonitor.measurement.application.port.out;

import com.energymonitor.measurement.api.MeasurementRecorded;

/**
 * Output port for publishing measurement events.
 *
 * <p>Keeps the application layer unaware of the event mechanism: the adapter decides
 * whether the event goes to the Spring context, a message broker or a log.
 */
public interface MeasurementEventPort {

    /**
     * Publishes that a measurement was recorded.
     *
     * @param event the event payload
     */
    void publish(MeasurementRecorded event);
}
