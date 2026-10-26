package com.energymonitor.home.adapter.out.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.energymonitor.home.adapter.out.remote.HomeServiceClient.RemoteAccess;
import com.energymonitor.home.adapter.out.remote.HomeServiceClient.RemoteHome;
import com.energymonitor.home.adapter.out.remote.HomeServiceClient.RemoteLimits;
import com.energymonitor.home.api.HomeDto;
import com.energymonitor.home.api.HomeServiceUnavailableException;
import com.energymonitor.home.api.HomeThresholdsDto;
import com.energymonitor.home.api.LimitPeriod;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the cached {@code HomeApi} asks the home service, when it asks again, and what it serves
 * while the service does not answer. The clock of the caches is moved by hand.
 */
@DisplayName("Home API answered by the home service")
class RemoteHomeApiTest {

    private static final Instant CREATED = Instant.parse("2026-01-15T10:30:00Z");
    private static final RemoteHome HOME = new RemoteHome("hom0000001", "Casa", "hous000001",
            "Calle 1", CREATED, List.of("use0000001", "use0000002"));
    private static final HomeServiceUnavailableException DOWN =
            new HomeServiceUnavailableException("home service did not answer", null);

    private final HomeServiceClient client = mock(HomeServiceClient.class);
    private final AtomicLong nanos = new AtomicLong();
    private final RemoteHomeApi api = new RemoteHomeApi(client, Duration.ofSeconds(30),
            Duration.ofHours(1), nanos::get);

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    @DisplayName("the home, its members and whether it exists come from one cached request")
    void homeFromOneRequest() {
        given(client.home("hom0000001")).willReturn(Optional.of(HOME));

        assertThat(api.findHome("hom0000001"))
                .contains(new HomeDto("hom0000001", "Casa", "hous000001", "Calle 1", CREATED));
        assertThat(api.memberIds("hom0000001")).containsExactly("use0000001", "use0000002");
        assertThat(api.homeExists("hom0000001")).isTrue();

        verify(client, times(1)).home("hom0000001");
    }

    @Test
    @DisplayName("a missing home is cached too, and has no members")
    void missingHomeCached() {
        given(client.home("hom0000099")).willReturn(Optional.empty());

        assertThat(api.homeExists("hom0000099")).isFalse();
        assertThat(api.memberIds("hom0000099")).isEmpty();
        assertThat(api.findHome("hom0000099")).isEmpty();

        verify(client, times(1)).home("hom0000099");
    }

    @Test
    @DisplayName("asks again once the entry expires")
    void asksAgainAfterTtl() {
        given(client.home("hom0000001")).willReturn(Optional.of(HOME));

        api.findHome("hom0000001");
        advance(Duration.ofSeconds(31));
        api.findHome("hom0000001");

        verify(client, times(2)).home("hom0000001");
    }

    @Test
    @DisplayName("serves the last home while the service does not answer")
    void staleHomeWhileDown() {
        given(client.home("hom0000001")).willReturn(Optional.of(HOME)).willThrow(DOWN);

        api.findHome("hom0000001");
        advance(Duration.ofMinutes(5));

        assertThat(api.memberIds("hom0000001")).containsExactly("use0000001", "use0000002");
    }

    @Test
    @DisplayName("fails when the service does not answer and nothing was kept")
    void failsWithNothingKept() {
        given(client.home("hom0000001")).willThrow(DOWN);

        assertThatThrownBy(() -> api.findHome("hom0000001")).isSameAs(DOWN);
    }

    @Test
    @DisplayName("the limits and their period, without the fields the service does not expose")
    void limits() {
        given(client.limits("hom0000001"))
                .willReturn(Optional.of(new RemoteLimits(6.0, 180.0, LimitPeriod.MONTHLY)));

        assertThat(api.findThresholds("hom0000001")).contains(
                new HomeThresholdsDto(null, "hom0000001", 6.0, 180.0, false, LimitPeriod.MONTHLY));
        api.findThresholds("hom0000001");

        verify(client, times(1)).limits("hom0000001");
    }

    @Test
    @DisplayName("serves the last limits while the service does not answer")
    void staleLimitsWhileDown() {
        given(client.limits("hom0000001"))
                .willReturn(Optional.of(new RemoteLimits(6.0, 180.0, LimitPeriod.DAILY)))
                .willThrow(DOWN);

        api.findThresholds("hom0000001");
        advance(Duration.ofMinutes(5));

        assertThat(api.findThresholds("hom0000001")).isPresent();
    }

    @Test
    @DisplayName("membership and ownership come from one cached request per user and home")
    void access() {
        given(client.access("hom0000001", "use0000002")).willReturn(new RemoteAccess(true, true, false));

        assertThat(api.isMember("use0000002", "hom0000001")).isTrue();
        assertThat(api.isOwner("use0000002", "hom0000001")).isFalse();

        verify(client, times(1)).access("hom0000001", "use0000002");
    }

    @Test
    @DisplayName("what a user may do in a home is never served stale")
    void accessNeverStale() {
        given(client.access("hom0000001", "use0000002"))
                .willReturn(new RemoteAccess(true, true, false))
                .willThrow(DOWN);

        assertThat(api.isMember("use0000002", "hom0000001")).isTrue();
        advance(Duration.ofSeconds(31));

        assertThatThrownBy(() -> api.isMember("use0000002", "hom0000001")).isSameAs(DOWN);
    }

    @Test
    @DisplayName("asks nothing without a home or a user")
    void nothingWithoutIdentifiers() {
        assertThat(api.findHome(null)).isEmpty();
        assertThat(api.memberIds(null)).isEmpty();
        assertThat(api.findThresholds(null)).isEmpty();
        assertThat(api.isMember(null, "hom0000001")).isFalse();
        assertThat(api.isOwner("use0000001", null)).isFalse();

        verifyNoInteractions(client);
    }
}
