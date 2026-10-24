package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.home.adapter.out.messaging.HomeMessageTypes;
import com.energymonitor.home.adapter.out.persistence.repository.HomeOutboxRepository;
import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.port.in.DeleteHome;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Home deletion against the real schema: memberships, thresholds and the home go in one
 * transaction, together with the {@code home.deleted} message the device service unlinks the
 * home's modules from. A refused deletion leaves everything, the outbox included.
 *
 * <p>{@code @Transactional}: everything joins the test's transaction, which is rolled back.
 */
@SpringBootTest
@Transactional
class HomeDeletionIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Autowired
    private DeleteHome deleteHome;
    @Autowired
    private HomePersistencePort homes;
    @Autowired
    private UserHomePersistencePort memberships;
    @Autowired
    private HomeThresholdsPersistencePort thresholds;
    @Autowired
    private HomeOutboxRepository outbox;

    @BeforeEach
    void seed() {
        homes.save(Home.create("hdh0000001", "Casa", "hous000001", "Calle 1", "HDH00001", null, NOW));
        thresholds.save(HomeThresholds.create("hdt0000001", "hdh0000001", 6.0, 180.0, true));
        memberships.save(UserHome.owner("hdu0000001", "hdh0000001"));
        memberships.save(UserHome.member("hdu0000002", "hdh0000001"));
    }

    @Test
    @DisplayName("deleting a home drops memberships and thresholds and announces it to the device service")
    void deletesTheHomeAndAnnouncesIt() {
        deleteHome.delete(new DeleteHomeCommand("hdu0000001", "hdh0000001"));

        assertThat(homes.findActive("hdh0000001")).isEmpty();
        assertThat(memberships.listActiveByHomeId("hdh0000001")).isEmpty();
        assertThat(thresholds.findActiveByHomeId("hdh0000001")).isEmpty();
        assertThat(homeDeletedMessages()).isEqualTo(1);
    }

    @Test
    @DisplayName("a member's attempt changes nothing and announces nothing")
    void memberCannotDelete() {
        assertThatThrownBy(() -> deleteHome.delete(new DeleteHomeCommand("hdu0000002", "hdh0000001")))
                .isInstanceOf(HomeAccessDeniedException.class);

        assertThat(homes.findActive("hdh0000001")).isPresent();
        assertThat(memberships.listActiveByHomeId("hdh0000001")).hasSize(2);
        assertThat(homeDeletedMessages()).isZero();
    }

    private long homeDeletedMessages() {
        return outbox.findAll().stream()
                .filter(m -> m.getMessageType().equals(HomeMessageTypes.HOME_DELETED))
                .filter(m -> m.getPayload().contains("\"hdh0000001\""))
                .count();
    }
}
