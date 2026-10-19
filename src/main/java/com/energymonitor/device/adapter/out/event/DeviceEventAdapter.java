package com.energymonitor.device.adapter.out.event;

import com.energymonitor.device.api.DeviceConnectivityLost;
import com.energymonitor.device.api.DeviceConnectivityRestored;
import com.energymonitor.device.api.DeviceLinked;
import com.energymonitor.device.api.DeviceUnlinked;
import com.energymonitor.device.application.port.out.DeviceEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes device events to the Spring application context.
 */
@Component
public class DeviceEventAdapter implements DeviceEventPort {

    private final ApplicationEventPublisher publisher;

    public DeviceEventAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DeviceConnectivityLost event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publish(DeviceConnectivityRestored event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publish(DeviceLinked event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publish(DeviceUnlinked event) {
        publisher.publishEvent(event);
    }
}
