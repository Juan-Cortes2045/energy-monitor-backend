package com.energymonitor.measurement.adapter.in.messaging;

import com.energymonitor.measurement.api.MeasurementRecorded;
import com.energymonitor.measurement.infrastructure.MeasurementEventsMessagingConfiguration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Hears {@code measurement.recorded} on {@code energy-monitor.measurement} and publishes it again
 * in memory as {@link MeasurementRecorded}, the same event the alert module has always listened
 * to. It runs in a transaction of its own, the alert's writes included.
 *
 * <p>A message may arrive twice: a threshold alert already pending is not raised again, and the
 * limit and linked-device checks look at what is stored, so a second delivery changes nothing. A
 * message without a device or a time is dropped; one that fails (unreadable, or a level that
 * cannot be read) goes back to the broker, which retries it and then dead-letters it.
 */
public class MeasurementRecordedConsumer {

    static final String MEASUREMENT_RECORDED = "measurement.recorded";

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementRecordedConsumer.class);

    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final ObjectMapper json;

    public MeasurementRecordedConsumer(ApplicationEventPublisher events, PlatformTransactionManager transactions,
                                       ObjectMapper json) {
        this.events = events;
        this.transaction = new TransactionTemplate(transactions);
        this.json = json;
    }

    @RabbitListener(queues = MeasurementEventsMessagingConfiguration.MEASUREMENT_RECORDED_QUEUE)
    public void on(Message message) {
        String type = typeOf(message);
        if (!MEASUREMENT_RECORDED.equals(type)) {
            LOG.warn("Ignoring measurement message of type '{}'", type);
            return;
        }
        Reading reading = json.readValue(message.getBody(), Reading.class);
        if (reading.deviceId() == null || reading.deviceId().isBlank() || reading.dateTime() == null) {
            LOG.warn("Ignoring {} {} without a device or a time", type, message.getMessageProperties().getMessageId());
            return;
        }
        transaction.executeWithoutResult(status -> events.publishEvent(new MeasurementRecorded(
                reading.idMeasurement(), reading.deviceId(), reading.dateTime(), reading.voltage(),
                reading.current(), reading.activePower(), reading.storedEnergy())));
    }

    /** The routing key it was published with, which the measurement service also sets as the type. */
    private static String typeOf(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        return routingKey != null ? routingKey : message.getMessageProperties().getType();
    }

    /** The body of {@code measurement.recorded}. */
    record Reading(String idMeasurement, String deviceId, Instant dateTime, double voltage, double current,
                   double activePower, double storedEnergy) {
    }
}
