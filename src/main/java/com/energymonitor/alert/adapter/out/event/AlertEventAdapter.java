package com.energymonitor.alert.adapter.out.event;

import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.alert.application.port.out.AlertEventPort;
import com.energymonitor.outbox.api.OutboxWriter;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Announces a raised alert as {@code alert.raised} on the topic exchange
 * {@code messaging.alert-exchange}, through the outbox, for the notification service, which
 * delivers it to the members of the home.
 *
 * <p>The body of {@code alert.raised} is the contract with the notification service:
 * {@code {idAlert, homeId, deviceId, type, messageKey, dateTime}}.
 */
@Component
public class AlertEventAdapter implements AlertEventPort {

    static final String ALERT_RAISED = "alert.raised";

    private final OutboxWriter outbox;
    private final String exchange;

    public AlertEventAdapter(OutboxWriter outbox, @Value("${messaging.alert-exchange}") String exchange) {
        this.outbox = outbox;
        this.exchange = exchange;
    }

    @Override
    public void publish(AlertRaised event) {
        outbox.write(exchange, ALERT_RAISED, new AlertRaisedMessage(event.idAlert(), event.homeId(),
                event.deviceId(), event.type().name(), event.messageKey(), event.dateTime()));
    }

    /**
     * Body of {@code alert.raised}.
     *
     * @param deviceId null when the alert is about the whole home
     */
    record AlertRaisedMessage(String idAlert, String homeId, String deviceId, String type,
                              String messageKey, Instant dateTime) {
    }
}
