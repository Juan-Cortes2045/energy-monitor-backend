package com.energymonitor.alert.application.usecase;

import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.alert.application.port.in.RegisterDeviceLinkedAlert;
import com.energymonitor.alert.application.port.out.AlertEventPort;
import com.energymonitor.alert.application.port.out.AlertPersistencePort;
import com.energymonitor.alert.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.alert.domain.model.Alert;
import java.time.Instant;

/**
 * Raises {@code alert.device.linked} each time a module is linked to a home, so every member
 * learns about it through the inbox, mail and push.
 *
 * <p>A single write, so no transaction is needed. This service contains no Spring annotations.
 */
public class RegisterDeviceLinkedAlertService implements RegisterDeviceLinkedAlert {

    static final String MESSAGE_KEY = "alert.device.linked";

    private final AlertPersistencePort alertPort;
    private final IdentifierGeneratorPort identifiers;
    private final AlertEventPort events;

    public RegisterDeviceLinkedAlertService(AlertPersistencePort alertPort, IdentifierGeneratorPort identifiers,
                                            AlertEventPort events) {
        this.alertPort = alertPort;
        this.identifiers = identifiers;
        this.events = events;
    }

    @Override
    public void register(String deviceId, String homeId, Instant dateTime) {
        Alert alert = Alert.device(identifiers.generate(), homeId, deviceId, MESSAGE_KEY, dateTime);
        alertPort.save(alert);
        events.publish(new AlertRaised(alert.idAlert(), alert.homeId(), alert.deviceId(), alert.type(),
                alert.messageKey(), alert.dateTime(), null, null));
    }
}
