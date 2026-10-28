package com.energymonitor.device.adapter.in.messaging;

import com.energymonitor.device.adapter.out.remote.DeviceChanges;
import com.energymonitor.device.infrastructure.DeviceEventsMessagingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import tools.jackson.databind.ObjectMapper;

/**
 * Hears {@code device.linked} and {@code device.unlinked} on {@code energy-monitor.device}: they
 * drop what this backend keeps of the device and of the home, so the recommendations of a home
 * see a device just linked or unlinked without waiting for the cache to expire.
 *
 * <p>The connectivity events belong to the alert service. A queue declared by an earlier version
 * of this backend may still be bound to them until it is recreated; they are dropped quietly.
 *
 * <p>A message may arrive twice: forgetting twice only costs one more request. An unknown type,
 * or a message without a device, is dropped.
 */
public class DeviceEventsConsumer {

    static final String DEVICE_LINKED = "device.linked";
    static final String DEVICE_UNLINKED = "device.unlinked";
    static final String CONNECTIVITY_PREFIX = "device.connectivity-";

    private static final Logger LOG = LoggerFactory.getLogger(DeviceEventsConsumer.class);

    private final DeviceChanges changes;
    private final ObjectMapper json;

    public DeviceEventsConsumer(DeviceChanges changes, ObjectMapper json) {
        this.changes = changes;
        this.json = json;
    }

    @RabbitListener(queues = DeviceEventsMessagingConfiguration.DEVICE_EVENTS_QUEUE)
    public void on(Message message) {
        String type = typeOf(message);
        if (type != null && type.startsWith(CONNECTIVITY_PREFIX)) {
            LOG.debug("Ignoring {}: the alert service handles it", type);
            return;
        }
        DeviceMessage body = json.readValue(message.getBody(), DeviceMessage.class);
        if (body.deviceId() == null || body.deviceId().isBlank()) {
            LOG.warn("Ignoring {} {} without a deviceId", type, message.getMessageProperties().getMessageId());
            return;
        }
        switch (type == null ? "" : type) {
            case DEVICE_LINKED, DEVICE_UNLINKED -> changes.forget(body.deviceId(), body.homeId());
            default -> LOG.warn("Ignoring device message of type '{}'", type);
        }
    }

    /** The routing key it was published with, which the device service also sets as the type. */
    private static String typeOf(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        return routingKey != null ? routingKey : message.getMessageProperties().getType();
    }

    /** The fields of a link or an unlink this backend reads. */
    record DeviceMessage(String deviceId, String homeId) {
    }
}
