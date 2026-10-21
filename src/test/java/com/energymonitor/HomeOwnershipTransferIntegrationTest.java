package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.Device;
import com.energymonitor.device.domain.model.DeviceHome;
import com.energymonitor.home.api.HomeOwnershipTransferred;
import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.command.TransferHomeOwnershipCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.port.in.DeleteHome;
import com.energymonitor.home.application.port.in.TransferHomeOwnership;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ownership transfer against the real schema: the roles are swapped in the database (including
 * the {@code SELECT ... FOR UPDATE} of the memberships), the event is published, and nothing that
 * belongs to the home changes.
 *
 * <p>{@code @Transactional}: everything joins the test's transaction, which is rolled back.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class HomeOwnershipTransferIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final String HOME = "oth0000001";
    private static final String OWNER = "otu0000001";
    private static final String MEMBER = "otu0000002";
    private static final String OUTSIDER = "otu0000003";

    @Autowired
    private TransferHomeOwnership transferOwnership;
    @Autowired
    private DeleteHome deleteHome;
    @Autowired
    private HomePersistencePort homes;
    @Autowired
    private UserHomePersistencePort memberships;
    @Autowired
    private HomeThresholdsPersistencePort thresholds;
    @Autowired
    private DevicePersistencePort devices;
    @Autowired
    private DeviceHomePersistencePort deviceHomes;
    @Autowired
    private DeviceApi deviceApi;
    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void seed() {
        homes.save(Home.create(HOME, "Casa", "hous000001", "Calle 1", "OTH00001", null, NOW));
        thresholds.save(HomeThresholds.create("ott0000001", HOME, 6.0, 180.0, true));
        memberships.save(UserHome.owner(OWNER, HOME));
        memberships.save(UserHome.member(MEMBER, HOME));
        devices.save(Device.create("otd0000001", "Nevera", "appl000001", "kitchen", null, NOW, "OT0001",
                "ot-device-api-key"));
        deviceHomes.save(new DeviceHome("otd0000001", HOME));
    }

    @Test
    @DisplayName("the member becomes the owner, the owner a member, and the home keeps everything")
    void transfersTheHome() {
        transferOwnership.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, MEMBER));

        assertThat(memberships.findActive(MEMBER, HOME)).get().returns(Role.OWNER, UserHome::role);
        assertThat(memberships.findActive(OWNER, HOME)).get().returns(Role.MEMBER, UserHome::role);
        assertThat(memberships.countActiveOwnersByHomeId(HOME)).isEqualTo(1);
        assertThat(events.stream(HomeOwnershipTransferred.class))
                .singleElement()
                .returns(OWNER, HomeOwnershipTransferred::previousOwnerId)
                .returns(MEMBER, HomeOwnershipTransferred::newOwnerId);

        assertThat(thresholds.findActiveByHomeId(HOME)).isPresent();
        assertThat(deviceApi.getHomeId("otd0000001")).contains(HOME);
        assertThat(deviceApi.validateCredentials("OT0001", "ot-device-api-key")).isTrue();
    }

    @Test
    @DisplayName("the previous owner can no longer transfer the home")
    void previousOwnerIsNowAMember() {
        transferOwnership.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, MEMBER));

        assertThatThrownBy(() -> transferOwnership.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, MEMBER)))
                .isInstanceOf(HomeAccessDeniedException.class);
        assertThat(memberships.countActiveOwnersByHomeId(HOME)).isEqualTo(1);
    }

    @Test
    @DisplayName("a member cannot transfer the home, and nobody outside it can receive it")
    void refusals() {
        assertThatThrownBy(() -> transferOwnership.transfer(new TransferHomeOwnershipCommand(MEMBER, HOME, MEMBER)))
                .isInstanceOf(HomeAccessDeniedException.class);
        assertThatThrownBy(() -> transferOwnership.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, OUTSIDER)))
                .isInstanceOf(UserHomeNotFoundException.class);

        assertThat(memberships.findActive(OWNER, HOME)).get().returns(Role.OWNER, UserHome::role);
        assertThat(memberships.findActive(OUTSIDER, HOME)).isEmpty();
        assertThat(events.stream(HomeOwnershipTransferred.class)).isEmpty();
    }

    @Test
    @DisplayName("a deleted home cannot be transferred")
    void deletedHome() {
        deleteHome.delete(new DeleteHomeCommand(OWNER, HOME));

        assertThatThrownBy(() -> transferOwnership.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, MEMBER)))
                .isInstanceOf(HomeNotFoundException.class);
        assertThat(memberships.findActive(MEMBER, HOME)).isEmpty();
        assertThat(events.stream(HomeOwnershipTransferred.class)).isEmpty();
    }
}
