package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.Device;
import com.energymonitor.device.domain.model.DeviceHome;
import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.port.in.DeleteHome;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.PersonPersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.Person;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Home deletion across modules, against the real schema: the home module deletes the home and
 * the device module, listening to {@code HomeDeleted}, unlinks its modules in the same
 * transaction.
 *
 * <p>{@code @Transactional}: everything joins the test's transaction, which is rolled back. That
 * also means the after-commit MQTT command is never sent, so no broker is needed.
 */
@SpringBootTest
@Transactional
class HomeDeletionIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Autowired
    private DeleteHome deleteHome;
    @Autowired
    private UserPersistencePort users;
    @Autowired
    private PersonPersistencePort persons;
    @Autowired
    private PasswordHasherPort hasher;
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

    private void seedUser(String idUser, String idPerson, String email) {
        persons.save(new Person(idPerson, "Ada", "Lovelace"));
        users.save(User.register(idUser, idPerson, hasher.hash("StrongPass1!"), Email.of(email), NOW, null));
    }

    @BeforeEach
    void seed() {
        seedUser("hdu0000001", "hdp0000001", "hd.owner@example.com");
        seedUser("hdu0000002", "hdp0000002", "hd.member@example.com");
        homes.save(Home.create("hdh0000001", "Casa", "hous000001", "Calle 1", "HDH00001", null, NOW));
        thresholds.save(HomeThresholds.create("hdt0000001", "hdh0000001", 6.0, 180.0, true));
        memberships.save(UserHome.owner("hdu0000001", "hdh0000001"));
        memberships.save(UserHome.member("hdu0000002", "hdh0000001"));
        devices.save(Device.create("hdd0000001", "Nevera", "appl000001", "kitchen", null, NOW, "HD0001",
                "hd-device-api-key"));
        deviceHomes.save(new DeviceHome("hdd0000001", "hdh0000001"));
    }

    @Test
    @DisplayName("deleting a home drops memberships and thresholds and unlinks its devices")
    void deletesTheHomeAndUnlinksItsDevices() {
        deleteHome.delete(new DeleteHomeCommand("hdu0000001", "hdh0000001"));

        assertThat(homes.findActive("hdh0000001")).isEmpty();
        assertThat(memberships.listActiveByHomeId("hdh0000001")).isEmpty();
        assertThat(thresholds.findActiveByHomeId("hdh0000001")).isEmpty();
        assertThat(deviceApi.getHomeId("hdd0000001")).isEmpty();
        // The module survives the home, with a new key, ready to be linked elsewhere.
        assertThat(devices.findById("hdd0000001")).hasValueSatisfying(
                device -> assertThat(device.apiKey()).isNotEqualTo("hd-device-api-key"));
    }

    @Test
    @DisplayName("a member's attempt changes nothing in any module")
    void memberCannotDelete() {
        assertThatThrownBy(() -> deleteHome.delete(new DeleteHomeCommand("hdu0000002", "hdh0000001")))
                .isInstanceOf(HomeAccessDeniedException.class);

        assertThat(homes.findActive("hdh0000001")).isPresent();
        assertThat(deviceApi.getHomeId("hdd0000001")).contains("hdh0000001");
    }
}
