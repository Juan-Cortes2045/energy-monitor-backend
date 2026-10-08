package com.energymonitor.notification.application.usecase;

/**
 * Wording of the alert notifications (mail and push), in Spanish like the rest of the mail.
 *
 * <p>The key is the {@code message_key} of the alert. An unknown key still produces a generic
 * message rather than nothing, so a new alert kind is never silently dropped.
 */
public final class AlertMessages {

    private AlertMessages() {
    }

    /**
     * @param title short line: mail subject and push title
     * @param body  one or two sentences, plain text
     */
    public record Message(String title, String body) {
    }

    public static Message render(String messageKey, String homeName, String deviceName) {
        String home = homeName == null ? "tu hogar" : "«" + homeName + "»";
        String device = deviceName == null ? "un dispositivo" : "«" + deviceName + "»";
        return switch (messageKey) {
            case "alert.device.linked" -> new Message("Nuevo dispositivo vinculado",
                    "Se vinculó " + device + " en " + home + ". Ya está enviando mediciones.");
            case "alert.connectivity.offline" -> new Message("Dispositivo desconectado",
                    device + " de " + home + " dejó de enviar datos. Revisa que siga enchufado y con Wi-Fi.");
            case "alert.threshold.critical" -> new Message("Consumo crítico",
                    device + " de " + home + " está consumiendo a un nivel crítico.");
            case "alert.threshold.high" -> new Message("Consumo alto",
                    device + " de " + home + " está consumiendo por encima de lo habitual.");
            default -> new Message("Nueva alerta", "Hay una nueva alerta en " + home + ".");
        };
    }
}
