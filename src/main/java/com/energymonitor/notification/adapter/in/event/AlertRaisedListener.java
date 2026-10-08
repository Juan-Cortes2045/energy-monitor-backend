package com.energymonitor.notification.adapter.in.event;

import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.notification.application.usecase.ManageNotificationChannels;
import com.energymonitor.notification.application.usecase.NotifyAlert;
import com.energymonitor.security.api.AccountDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends every alert by mail and push, and forgets a deleted account's channels.
 *
 * <p>Asynchronous: SMTP and push services take seconds, and the alert is raised on the MQTT
 * consumer thread or inside a web request, neither of which should wait for them.
 */
@Component
public class AlertRaisedListener {

    private final NotifyAlert notifyAlert;
    private final ManageNotificationChannels channels;

    public AlertRaisedListener(NotifyAlert notifyAlert, ManageNotificationChannels channels) {
        this.notifyAlert = notifyAlert;
        this.channels = channels;
    }

    @Async
    @EventListener
    public void on(AlertRaised event) {
        notifyAlert.notify(event.idAlert(), event.homeId(), event.deviceId(), event.messageKey());
    }

    @EventListener
    public void on(AccountDeleted event) {
        channels.forget(event.userId());
    }
}
