package com.energymonitor.home.adapter.out.event;

import com.energymonitor.home.adapter.out.messaging.HomeMessagePayloads.HomeDeletedPayload;
import com.energymonitor.home.adapter.out.messaging.HomeMessageTypes;
import com.energymonitor.home.adapter.out.messaging.HomeOutboxWriter;
import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.api.HomeOwnershipTransferred;
import com.energymonitor.home.application.port.out.HomeEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes home events to the Spring application context, and the ones other services need to
 * the outbox as well.
 *
 * <p>{@link HomeDeleted} goes to both: the modules of this backend still hear it in memory, and
 * the device service hears it through the broker ({@code home.deleted}). The use cases publish it
 * inside the transaction that deletes the home, so the outbox row commits with the deletion.
 */
@Component
public class HomeEventAdapter implements HomeEventPort {

    private final ApplicationEventPublisher publisher;
    private final HomeOutboxWriter outbox;

    public HomeEventAdapter(ApplicationEventPublisher publisher, HomeOutboxWriter outbox) {
        this.publisher = publisher;
        this.outbox = outbox;
    }

    @Override
    public void publish(HomeDeleted event) {
        publisher.publishEvent(event);
        outbox.write(HomeMessageTypes.HOME_DELETED,
                new HomeDeletedPayload(event.homeId(), event.deletedBy(), event.occurredAt()),
                event.occurredAt());
    }

    @Override
    public void publish(HomeOwnershipTransferred event) {
        publisher.publishEvent(event);
    }
}
