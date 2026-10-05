package com.energymonitor.alert.adapter.out.event;

import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.alert.application.port.out.AlertEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes alert events to the Spring application context.
 *
 * <p>The {@code notification} module is expected to subscribe with {@code @EventListener}
 * or, once it needs delivery guarantees, with Spring Modulith's
 * {@code @ApplicationModuleListener}.
 */
@Component
public class AlertEventAdapter implements AlertEventPort {

    private final ApplicationEventPublisher publisher;

    public AlertEventAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(AlertRaised event) {
        publisher.publishEvent(event);
    }
}
