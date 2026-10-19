package com.energymonitor.alert.adapter.in.event;

import com.energymonitor.alert.application.command.RegisterConnectivityAlertCommand;
import com.energymonitor.alert.application.port.in.RegisterConnectivityAlert;
import com.energymonitor.alert.application.port.in.ResolveConnectivityAlerts;
import com.energymonitor.device.api.DeviceConnectivityLost;
import com.energymonitor.device.api.DeviceConnectivityRestored;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns the device module's {@code DeviceConnectivityLost} into a CONNECTIVITY alert, and its
 * {@code DeviceConnectivityRestored} into the resolution of that alert.
 */
@Component
public class DeviceConnectivityLostListener {

    private final RegisterConnectivityAlert registerConnectivityAlert;
    private final ResolveConnectivityAlerts resolveConnectivityAlerts;

    public DeviceConnectivityLostListener(RegisterConnectivityAlert registerConnectivityAlert,
                                          ResolveConnectivityAlerts resolveConnectivityAlerts) {
        this.registerConnectivityAlert = registerConnectivityAlert;
        this.resolveConnectivityAlerts = resolveConnectivityAlerts;
    }

    @EventListener
    public void on(DeviceConnectivityLost event) {
        registerConnectivityAlert.register(new RegisterConnectivityAlertCommand(event.deviceId(), event.occurredAt()));
    }

    @EventListener
    public void on(DeviceConnectivityRestored event) {
        resolveConnectivityAlerts.resolveFor(event.deviceId());
    }
}
