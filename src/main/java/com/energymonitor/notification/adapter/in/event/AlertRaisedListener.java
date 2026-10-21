package com.energymonitor.notification.adapter.in.event;

import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.notification.application.usecase.NotifyAlert;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends every alert by mail and push.
 *
 * <p>Asynchronous: SMTP and push services take seconds, and the alert is raised on the MQTT
 * consumer thread or inside a web request, neither of which should wait for them.
 */
@Component
public class AlertRaisedListener {

    private final NotifyAlert notifyAlert;

    public AlertRaisedListener(NotifyAlert notifyAlert) {
        this.notifyAlert = notifyAlert;
    }

    @Async
    @EventListener
    public void on(AlertRaised event) {
        notifyAlert.notify(event.idAlert(), event.homeId(), event.deviceId(), event.messageKey());
    }
}
