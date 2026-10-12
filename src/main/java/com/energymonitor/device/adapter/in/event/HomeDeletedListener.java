package com.energymonitor.device.adapter.in.event;

import com.energymonitor.device.application.port.in.UnlinkHomeDevices;
import com.energymonitor.home.api.HomeDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Unlinks the modules of a deleted home.
 *
 * <p>Synchronous on purpose: it runs inside the transaction that deletes the home, so the home and
 * its links disappear together or not at all. The commands to the modules (MQTT) are sent after
 * that transaction commits, by {@link DeviceCommandListener}.
 */
@Component
public class HomeDeletedListener {

    private final UnlinkHomeDevices unlinkHomeDevices;

    public HomeDeletedListener(UnlinkHomeDevices unlinkHomeDevices) {
        this.unlinkHomeDevices = unlinkHomeDevices;
    }

    @EventListener
    public void on(HomeDeleted event) {
        unlinkHomeDevices.unlinkAll(event.homeId());
    }
}
