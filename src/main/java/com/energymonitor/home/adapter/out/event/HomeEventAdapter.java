package com.energymonitor.home.adapter.out.event;

import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.api.HomeOwnershipTransferred;
import com.energymonitor.home.application.port.out.HomeEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes home events to the Spring application context (in memory, no broker).
 */
@Component
public class HomeEventAdapter implements HomeEventPort {

    private final ApplicationEventPublisher publisher;

    public HomeEventAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(HomeDeleted event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publish(HomeOwnershipTransferred event) {
        publisher.publishEvent(event);
    }
}
