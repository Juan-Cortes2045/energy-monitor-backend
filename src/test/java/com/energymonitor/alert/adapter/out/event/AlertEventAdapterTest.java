package com.energymonitor.alert.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.energymonitor.alert.adapter.out.event.AlertEventAdapter.AlertRaisedMessage;
import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.alert.api.AlertType;
import com.energymonitor.outbox.api.OutboxWriter;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("Alert events")
class AlertEventAdapterTest {

    private static final Instant AT = Instant.parse("2026-10-23T15:00:00Z");

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final AlertEventAdapter adapter =
            new AlertEventAdapter(publisher, outbox, "energy-monitor.alert");

    @Test
    @DisplayName("an alert goes to the outbox as alert.raised, with the body the notification service reads")
    void writesAlertRaised() {
        AlertRaised event = new AlertRaised("alr0000001", "hom0000001", "dev0000001",
                AlertType.THRESHOLD, "alert.threshold.high", AT, "lvl0000001", "mea0000001");

        adapter.publish(event);

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(outbox).write(eq("energy-monitor.alert"), eq("alert.raised"), body.capture());
        assertThat(body.getValue()).isEqualTo(new AlertRaisedMessage("alr0000001", "hom0000001",
                "dev0000001", "THRESHOLD", "alert.threshold.high", AT));
    }

    @Test
    @DisplayName("the alert is still announced in process, for the notification module of this backend")
    void stillPublishedInProcess() {
        AlertRaised event = new AlertRaised("alr0000002", "hom0000001", null, AlertType.LIMIT,
                "alert.limit.daily", AT, null, null);

        adapter.publish(event);

        verify(publisher).publishEvent(event);
        verify(outbox).write(eq("energy-monitor.alert"), eq("alert.raised"), any());
    }
}
