package com.energymonitor.notification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.energymonitor.notification.application.port.out.HomeDirectoryPort;
import com.energymonitor.notification.application.port.out.NotificationPreferencePort;
import com.energymonitor.notification.application.port.out.NotificationRecordPort;
import com.energymonitor.notification.application.port.out.PushSenderPort;
import com.energymonitor.notification.application.port.out.PushSubscriptionPort;
import com.energymonitor.notification.application.usecase.NotifyAlert;
import com.energymonitor.notification.domain.model.NotificationPreference;
import com.energymonitor.notification.domain.model.NotificationSourceType;
import com.energymonitor.notification.domain.model.PushSubscription;
import com.energymonitor.notification.infrastructure.mail.EmailSender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class NotifyAlertTest {

    private final Map<String, NotificationPreference> prefs = new HashMap<>();
    private final List<PushSubscription> subscriptions = new ArrayList<>();
    private final List<String> pushedTo = new ArrayList<>();
    private PushSenderPort.Outcome pushOutcome = PushSenderPort.Outcome.DELIVERED;
    private final EmailSender mailer = mock(EmailSender.class);
    private int ids;

    private final NotifyAlert notifyAlert = new NotifyAlert(
            new HomeDirectoryPort() {
                @Override
                public List<String> memberIds(String homeId) {
                    return List.of("usr0000001", "usr0000002");
                }

                @Override
                public Map<String, Recipient> recipients(List<String> userIds) {
                    return Map.of("usr0000001", new Recipient("usr0000001", "Ana", "ana@example.com"),
                            "usr0000002", new Recipient("usr0000002", "Luis", "luis@example.com"));
                }

                @Override
                public Optional<String> homeName(String homeId) {
                    return Optional.of("Casa");
                }

                @Override
                public Optional<String> deviceName(String deviceId) {
                    return Optional.of("Nevera");
                }
            },
            new NotificationPreferencePort() {
                @Override
                public Optional<NotificationPreference> find(String userId) {
                    return Optional.ofNullable(prefs.get(userId));
                }

                @Override
                public void save(NotificationPreference preference) {
                    prefs.put(preference.userId(), preference);
                }

                @Override
                public void forget(String userId) {
                    prefs.remove(userId);
                }
            },
            new PushSubscriptionPort() {
                @Override
                public List<PushSubscription> findByUser(String userId) {
                    return subscriptions.stream().filter(s -> s.userId().equals(userId)).toList();
                }

                @Override
                public Optional<PushSubscription> findByEndpoint(String endpoint) {
                    return subscriptions.stream().filter(s -> s.endpoint().equals(endpoint)).findFirst();
                }

                @Override
                public void save(PushSubscription subscription) {
                    subscriptions.add(subscription);
                }

                @Override
                public void deleteByEndpoint(String endpoint) {
                    subscriptions.removeIf(s -> s.endpoint().equals(endpoint));
                }
            },
            new PushSenderPort() {
                @Override
                public boolean isConfigured() {
                    return true;
                }

                @Override
                public String publicKey() {
                    return "key";
                }

                @Override
                public Result send(PushSubscription subscription, String jsonPayload) {
                    pushedTo.add(subscription.userId());
                    return new Result(pushOutcome, null);
                }
            },
            mailer,
            mock(NotificationRecordPort.class),
            () -> String.format("not%07d", ++ids),
            JsonMapper.builder().build(),
            "http://localhost:5173");

    private static PushSubscription browser(String userId, String endpoint) {
        return new PushSubscription("sub" + endpoint.length(), userId, endpoint, "p".repeat(87), "a".repeat(22));
    }

    @Test
    void everyMemberGetsMailByDefault() {
        notifyAlert.notify("ale0000001", "hom0000001", "dev0000001", "alert.device.linked");

        ArgumentCaptor<EmailSender.Delivery> mail = ArgumentCaptor.forClass(EmailSender.Delivery.class);
        verify(mailer, org.mockito.Mockito.times(2)).send(mail.capture());
        assertEquals(List.of("ana@example.com", "luis@example.com"),
                mail.getAllValues().stream().map(EmailSender.Delivery::recipient).sorted().toList());
        assertTrue(mail.getValue().text().contains("«Nevera»"));
    }

    @Test
    void preferencesAreRespectedPerMember() {
        prefs.put("usr0000001", new NotificationPreference("usr0000001", false, true));
        prefs.put("usr0000002", new NotificationPreference("usr0000002", false, false));
        subscriptions.add(browser("usr0000001", "https://push.example/a"));
        subscriptions.add(browser("usr0000002", "https://push.example/b"));

        notifyAlert.notify("ale0000001", "hom0000001", "dev0000001", "alert.connectivity.offline");

        verify(mailer, never()).send(any(EmailSender.Delivery.class));
        assertEquals(List.of("usr0000001"), pushedTo);
    }

    @Test
    void aSubscriptionThePushServiceForgotIsDeleted() {
        prefs.put("usr0000001", new NotificationPreference("usr0000001", false, true));
        subscriptions.add(browser("usr0000001", "https://push.example/a"));
        pushOutcome = PushSenderPort.Outcome.GONE;

        notifyAlert.notify("ale0000001", "hom0000001", "dev0000001", "alert.threshold.high");

        assertTrue(subscriptions.isEmpty());
    }

    @Test
    void recommendationsAreDeliveredAsTheirOwnSource() {
        notifyAlert.notify(NotificationSourceType.RECOMMENDATION, "rec0000001", "hom0000001", "dev0000001",
                "recommendation.standby");

        ArgumentCaptor<EmailSender.Delivery> mail = ArgumentCaptor.forClass(EmailSender.Delivery.class);
        verify(mailer, org.mockito.Mockito.times(2)).send(mail.capture());
        assertEquals(NotificationSourceType.RECOMMENDATION, mail.getValue().sourceType());
        assertEquals("rec0000001", mail.getValue().sourceId());
        assertTrue(mail.getValue().subject().contains("consumo en reposo"));
        assertTrue(mail.getValue().text().contains("«Nevera»"));
    }
}
