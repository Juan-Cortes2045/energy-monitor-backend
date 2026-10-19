package com.energymonitor.measurement.adapter.out.event;

import com.energymonitor.measurement.api.MeasurementRecorded;
import com.energymonitor.measurement.application.port.out.MeasurementEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes measurement events to the Spring application context.
 *
 * <p>Listeners in other modules (alert, recommendation) subscribe with
 * {@code @EventListener} or, once they need delivery guarantees, with Spring Modulith's
 * {@code @ApplicationModuleListener}. The publisher is unaware of them, which keeps the
 * dependency direction pointing at this module's API.
 */
@Component
public class MeasurementEventAdapter implements MeasurementEventPort {

    private final ApplicationEventPublisher publisher;

    public MeasurementEventAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(MeasurementRecorded event) {
        publisher.publishEvent(event);
    }
}
