package com.energymonitor.alert.adapter.in.event;

import com.energymonitor.alert.application.port.in.RegisterDeviceLinkedAlert;
import com.energymonitor.device.api.DeviceLinked;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns the device module's {@code DeviceLinked} into a DEVICE alert for the home.
 */
@Component
public class DeviceLinkedListener {

    private final RegisterDeviceLinkedAlert registerDeviceLinkedAlert;

    public DeviceLinkedListener(RegisterDeviceLinkedAlert registerDeviceLinkedAlert) {
        this.registerDeviceLinkedAlert = registerDeviceLinkedAlert;
    }

    @EventListener
    public void on(DeviceLinked event) {
        registerDeviceLinkedAlert.register(event.deviceId(), event.homeId(), event.occurredAt());
    }
}
