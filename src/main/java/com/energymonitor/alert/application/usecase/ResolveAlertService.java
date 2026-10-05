package com.energymonitor.alert.application.usecase;

import com.energymonitor.alert.application.command.ResolveAlertCommand;
import com.energymonitor.alert.application.exception.AlertNotFoundException;
import com.energymonitor.alert.application.port.in.ResolveAlert;
import com.energymonitor.alert.application.port.out.AlertPersistencePort;
import com.energymonitor.alert.application.result.AlertResult;

/**
 * Marks an alert as resolved.
 *
 * <p>A single write, so no transaction boundary. This service contains no Spring
 * annotations.
 */
public class ResolveAlertService implements ResolveAlert {

    private final AlertPersistencePort alertPort;

    public ResolveAlertService(AlertPersistencePort alertPort) {
        this.alertPort = alertPort;
    }

    @Override
    public AlertResult resolve(ResolveAlertCommand command) {
        var alert = alertPort.findActive(command.alertId())
                .orElseThrow(() -> new AlertNotFoundException("no alert " + command.alertId()));
        alert.resolve();
        alertPort.save(alert);
        return AlertResult.from(alert);
    }
}
