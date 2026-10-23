package com.energymonitor.home.adapter.out.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.energymonitor.home.adapter.out.messaging.HomeMessagePayloads.HomeDeletedPayload;
import com.energymonitor.home.adapter.out.messaging.HomeMessageTypes;
import com.energymonitor.home.adapter.out.messaging.HomeOutboxWriter;
import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.api.HomeOwnershipTransferred;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("Home event adapter")
class HomeEventAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-23T15:00:00Z");

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final HomeOutboxWriter outbox = mock(HomeOutboxWriter.class);
    private final HomeEventAdapter adapter = new HomeEventAdapter(publisher, outbox);

    @Test
    @DisplayName("a deleted home is announced in memory and to the outbox")
    void homeDeletedGoesToBoth() {
        var event = new HomeDeleted("hom0000001", "use0000001", NOW);

        adapter.publish(event);

        verify(publisher).publishEvent(event);
        verify(outbox).write(HomeMessageTypes.HOME_DELETED,
                new HomeDeletedPayload("hom0000001", "use0000001", NOW), NOW);
    }

    @Test
    @DisplayName("an ownership transfer stays in memory")
    void ownershipTransferStaysInMemory() {
        var event = new HomeOwnershipTransferred("hom0000001", "use0000001", "use0000002", NOW);

        adapter.publish(event);

        verify(publisher).publishEvent(event);
        verify(outbox, never()).write(anyString(), any(), any());
    }
}
