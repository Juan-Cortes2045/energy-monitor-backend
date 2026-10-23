package com.energymonitor.device.adapter.out.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.energymonitor.device.adapter.out.remote.DeviceServiceClient.RemoteDevice;
import com.energymonitor.device.api.ConnectivityStatus;
import com.energymonitor.device.api.DeviceDto;
import com.energymonitor.device.api.DeviceServiceUnavailableException;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the cached {@code DeviceApi} asks the device service, when it asks again, and what it
 * serves while the service does not answer. The clock of the caches is moved by hand.
 */
@DisplayName("Device API answered by the device service")
class RemoteDeviceApiTest {

    private static final Instant LINKED = Instant.parse("2026-10-01T10:00:00Z");
    private static final RemoteDevice FRIDGE = new RemoteDevice("DEV0000001", "Nevera", "appl000001",
            "Cocina", null, LINKED, "EM204", "hom0000001");

    private final DeviceServiceClient client = mock(DeviceServiceClient.class);
    private final AtomicLong nanos = new AtomicLong();
    private final RemoteDeviceApi api = new RemoteDeviceApi(client, Duration.ofSeconds(30),
            Duration.ofHours(1), nanos::get);

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    @DisplayName("a device and its home come from one cached request")
    void deviceAndHomeFromOneRequest() {
        given(client.device("DEV0000001")).willReturn(Optional.of(FRIDGE));

        assertThat(api.findByDeviceId("DEV0000001")).contains(new DeviceDto("DEV0000001", "Nevera",
                "appl000001", "Cocina", null, LINKED, "EM204"));
        assertThat(api.getHomeId("DEV0000001")).contains("hom0000001");
        assertThat(api.findByDeviceId("DEV0000001")).isPresent();

        verify(client, times(1)).device("DEV0000001");
    }

    @Test
    @DisplayName("an unknown device is cached too, so its readings do not ask every time")
    void unknownDeviceCached() {
        given(client.device("DEV0000099")).willReturn(Optional.empty());

        assertThat(api.findByDeviceId("DEV0000099")).isEmpty();
        assertThat(api.findByDeviceId("DEV0000099")).isEmpty();

        verify(client, times(1)).device("DEV0000099");
    }

    @Test
    @DisplayName("asks again once the entry expires")
    void asksAgainAfterTtl() {
        given(client.device("DEV0000001")).willReturn(Optional.of(FRIDGE));

        api.findByDeviceId("DEV0000001");
        advance(Duration.ofSeconds(31));
        api.findByDeviceId("DEV0000001");

        verify(client, times(2)).device("DEV0000001");
    }

    @Test
    @DisplayName("serves the last answer while the service does not answer")
    void servesStaleWhenUnavailable() {
        given(client.device("DEV0000001")).willReturn(Optional.of(FRIDGE))
                .willThrow(new DeviceServiceUnavailableException("down", null));
        given(client.devicesOfHome("hom0000001")).willReturn(List.of(FRIDGE))
                .willThrow(new DeviceServiceUnavailableException("down", null));

        api.findByDeviceId("DEV0000001");
        api.findByHome("hom0000001");
        advance(Duration.ofMinutes(10));

        assertThat(api.getHomeId("DEV0000001")).contains("hom0000001");
        assertThat(api.findByHome("hom0000001")).extracting(DeviceDto::idDevice).containsExactly("DEV0000001");
    }

    @Test
    @DisplayName("with nothing kept, an unavailable service fails the call")
    void failsWithNothingKept() {
        given(client.device("DEV0000001")).willThrow(new DeviceServiceUnavailableException("down", null));

        assertThatThrownBy(() -> api.findByDeviceId("DEV0000001"))
                .isInstanceOf(DeviceServiceUnavailableException.class);
    }

    @Test
    @DisplayName("the last answer is not kept forever")
    void staleExpires() {
        given(client.device("DEV0000001")).willReturn(Optional.of(FRIDGE))
                .willThrow(new DeviceServiceUnavailableException("down", null));

        api.findByDeviceId("DEV0000001");
        advance(Duration.ofHours(2));

        assertThatThrownBy(() -> api.findByDeviceId("DEV0000001"))
                .isInstanceOf(DeviceServiceUnavailableException.class);
    }

    @Test
    @DisplayName("the devices of a home are cached by home")
    void devicesOfHomeCached() {
        given(client.devicesOfHome("hom0000001")).willReturn(List.of(FRIDGE));

        assertThat(api.findByHome("hom0000001")).extracting(DeviceDto::name).containsExactly("Nevera");
        api.findByHome("hom0000001");
        assertThat(api.findByHome(null)).isEmpty();

        verify(client, times(1)).devicesOfHome("hom0000001");
    }

    @Test
    @DisplayName("forgetting a device and its home asks the service again at once")
    void forget() {
        given(client.device("DEV0000001")).willReturn(Optional.of(FRIDGE));
        given(client.devicesOfHome("hom0000001")).willReturn(List.of(FRIDGE));
        api.findByDeviceId("DEV0000001");
        api.findByHome("hom0000001");

        api.forget("DEV0000001", "hom0000001");
        api.findByDeviceId("DEV0000001");
        api.findByHome("hom0000001");

        verify(client, times(2)).device("DEV0000001");
        verify(client, times(2)).devicesOfHome("hom0000001");
    }

    @Test
    @DisplayName("no device asked is no request")
    void nothingAsked() {
        assertThat(api.findByDeviceId(null)).isEmpty();
        assertThat(api.getHomeId(null)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("connectivity and credentials belong to the device service")
    void notServedHere() {
        assertThatThrownBy(() -> api.recordConnectivity("DEV0000001", ConnectivityStatus.ONLINE, -58))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> api.validateCredentials("EM204", "key"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> api.findByDeviceCode("EM204"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
