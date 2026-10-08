package com.energymonitor.notification.adapter.in.web;

import com.energymonitor.notification.adapter.in.web.dto.NotificationPreferencesDto;
import com.energymonitor.notification.adapter.in.web.dto.PushSubscriptionRequest;
import com.energymonitor.notification.application.usecase.ManageNotificationChannels;
import com.energymonitor.notification.domain.model.NotificationPreference;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's notification channels: mail and push preferences, and push subscriptions.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationChannelsController {

    private final ManageNotificationChannels channels;
    private final CurrentUserResolver currentUser;

    public NotificationChannelsController(ManageNotificationChannels channels, CurrentUserResolver currentUser) {
        this.channels = channels;
        this.currentUser = currentUser;
    }

    @GetMapping("/preferences")
    public NotificationPreferencesDto preferences() {
        String userId = currentUser.resolveCurrentUserId();
        return toDto(userId, channels.preferences(userId));
    }

    @PutMapping("/preferences")
    public NotificationPreferencesDto update(@Valid @RequestBody NotificationPreferencesDto request) {
        String userId = currentUser.resolveCurrentUserId();
        return toDto(userId, channels.update(userId, request.emailEnabled(), request.pushEnabled()));
    }

    /** The VAPID public key a browser needs to subscribe; 404 when push is not configured. */
    @GetMapping("/push/public-key")
    public ResponseEntity<Map<String, String>> publicKey() {
        if (!channels.pushAvailable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("publicKey", channels.pushPublicKey()));
    }

    @PostMapping("/push/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        channels.subscribe(currentUser.resolveCurrentUserId(), request.endpoint(), request.keys().p256dh(),
                request.keys().auth());
    }

    @PostMapping("/push/subscriptions/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestBody Map<String, String> request) {
        String endpoint = request.get("endpoint");
        if (endpoint != null) {
            channels.unsubscribe(currentUser.resolveCurrentUserId(), endpoint);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    private NotificationPreferencesDto toDto(String userId, NotificationPreference preference) {
        return new NotificationPreferencesDto(preference.emailEnabled(), preference.pushEnabled(),
                channels.pushAvailable(), channels.browsers(userId));
    }
}
