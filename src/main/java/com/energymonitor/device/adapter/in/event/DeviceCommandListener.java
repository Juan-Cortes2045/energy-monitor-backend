package com.energymonitor.device.adapter.in.event;

import com.energymonitor.device.api.DeviceLinked;
import com.energymonitor.device.api.DeviceUnlinked;
import com.energymonitor.device.application.port.out.DeviceCommandPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the module its command once the change is committed: a module is never told it was
 * removed when the removal was rolled back.
 */
@Component
public class DeviceCommandListener {

    private final DeviceCommandPort commands;

    public DeviceCommandListener(DeviceCommandPort commands) {
        this.commands = commands;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceUnlinked event) {
        commands.sendUnlinked(event.deviceId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DeviceLinked event) {
        commands.clearCommands(event.deviceId());
    }
}
