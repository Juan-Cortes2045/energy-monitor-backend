package com.energymonitor.measurement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

@DisplayName("Measurement events queue")
class MeasurementEventsMessagingConfigurationTest {

    private final Declarables declarables = new MeasurementEventsMessagingConfiguration()
            .measurementEventQueues("energy-monitor.measurement", "energy-monitor-backend.dlx");

    @Test
    @DisplayName("the durable queue takes measurement.recorded from the measurement exchange")
    void bindsTheRecordedReadings() {
        assertThat(declarables.getDeclarablesByType(Binding.class).stream()
                .filter(b -> b.getDestination().equals(MeasurementEventsMessagingConfiguration.MEASUREMENT_RECORDED_QUEUE)))
                .allMatch(b -> b.getExchange().equals("energy-monitor.measurement"))
                .extracting(Binding::getRoutingKey)
                .containsExactly("measurement.recorded");
    }

    @Test
    @DisplayName("a message that keeps failing goes to its dead-letter queue")
    void deadLetters() {
        Queue queue = declarables.getDeclarablesByType(Queue.class).stream()
                .filter(q -> q.getName().equals(MeasurementEventsMessagingConfiguration.MEASUREMENT_RECORDED_QUEUE))
                .findFirst().orElseThrow();

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "energy-monitor-backend.dlx")
                .containsEntry("x-dead-letter-routing-key", MeasurementEventsMessagingConfiguration.MEASUREMENT_RECORDED_QUEUE);
    }
}
