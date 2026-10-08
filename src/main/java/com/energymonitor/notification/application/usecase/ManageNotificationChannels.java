package com.energymonitor.notification.application.usecase;

import com.energymonitor.notification.application.port.out.NotificationIdentifierPort;
import com.energymonitor.notification.application.port.out.NotificationPreferencePort;
import com.energymonitor.notification.application.port.out.PushSenderPort;
import com.energymonitor.notification.application.port.out.PushSubscriptionPort;
import com.energymonitor.notification.domain.model.NotificationPreference;
import com.energymonitor.notification.domain.model.PushSubscription;
import org.springframework.stereotype.Component;

/**
 * A user's channel preferences and the browsers subscribed to push.
 */
@Component
public class ManageNotificationChannels {

    private final NotificationPreferencePort preferences;
    private final PushSubscriptionPort subscriptions;
    private final PushSenderPort push;
    private final NotificationIdentifierPort identifiers;

    public ManageNotificationChannels(NotificationPreferencePort preferences, PushSubscriptionPort subscriptions,
                                      PushSenderPort push, NotificationIdentifierPort identifiers) {
        this.preferences = preferences;
        this.subscriptions = subscriptions;
        this.push = push;
        this.identifiers = identifiers;
    }

    public NotificationPreference preferences(String userId) {
        return preferences.find(userId).orElseGet(() -> NotificationPreference.defaults(userId));
    }

    public NotificationPreference update(String userId, boolean emailEnabled, boolean pushEnabled) {
        NotificationPreference updated = new NotificationPreference(userId, emailEnabled, pushEnabled);
        preferences.save(updated);
        return updated;
    }

    public boolean pushAvailable() {
        return push.isConfigured();
    }

    public String pushPublicKey() {
        return push.publicKey();
    }

    public int browsers(String userId) {
        return subscriptions.findByUser(userId).size();
    }

    public void subscribe(String userId, String endpoint, String p256dh, String auth) {
        String id = subscriptions.findByEndpoint(endpoint).map(PushSubscription::idSubscription)
                .orElseGet(identifiers::generate);
        subscriptions.save(new PushSubscription(id, userId, endpoint, p256dh, auth));
    }

    /** Only the owner of the subscription may drop it. */
    public void unsubscribe(String userId, String endpoint) {
        subscriptions.findByEndpoint(endpoint)
                .filter(s -> s.userId().equals(userId))
                .ifPresent(s -> subscriptions.deleteByEndpoint(endpoint));
    }

    public void forget(String userId) {
        preferences.forget(userId);
    }
}
