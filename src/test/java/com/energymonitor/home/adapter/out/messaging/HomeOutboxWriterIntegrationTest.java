package com.energymonitor.home.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.home.adapter.out.persistence.entity.HomeOutboxEntity;
import com.energymonitor.home.adapter.out.persistence.repository.HomeOutboxRepository;
import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.port.in.DeleteHome;
import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The {@code home.deleted} message is written in the transaction that deletes the home, against
 * the real schema. {@code @Transactional}: the test's transaction is rolled back, so no row is
 * left behind; the test profile also stores the rows already discarded.
 */
@SpringBootTest
@Transactional
@DisplayName("Home outbox")
class HomeOutboxWriterIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final String HOME = "hob0000001";
    private static final String OWNER = "hou0000001";
    private static final String MEMBER = "hou0000002";

    @Autowired
    private DeleteHome deleteHome;
    @Autowired
    private RemoveDeletedAccount removeDeletedAccount;
    @Autowired
    private HomePersistencePort homes;
    @Autowired
    private UserHomePersistencePort memberships;
    @Autowired
    private HomeThresholdsPersistencePort thresholds;
    @Autowired
    private HomeOutboxRepository outbox;
    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void seed() {
        homes.save(Home.create(HOME, "Casa", "hous000001", "Calle 1", "HOB00001", null, NOW));
        thresholds.save(HomeThresholds.create("hot0000001", HOME, 6.0, 180.0, true));
        memberships.save(UserHome.owner(OWNER, HOME));
    }

    @Test
    @DisplayName("deleting a home writes home.deleted with the home and who deleted it")
    void deletionWritesTheMessage() {
        deleteHome.delete(new DeleteHomeCommand(OWNER, HOME));

        List<HomeOutboxEntity> messages = messagesFor(HOME);
        assertThat(messages).hasSize(1);
        HomeOutboxEntity message = messages.getFirst();
        assertThat(message.getMessageType()).isEqualTo(HomeMessageTypes.HOME_DELETED);
        JsonNode payload = json.readTree(message.getPayload());
        assertThat(payload.get("homeId").asString()).isEqualTo(HOME);
        assertThat(payload.get("deletedBy").asString()).isEqualTo(OWNER);
        assertThat(payload.get("deletedAt").asString()).isNotBlank();
        assertThat(message.getPublishedAt()).isNull();
        assertThat(message.getDiscardedAt()).as("the test profile never lets it be published").isNotNull();
    }

    @Test
    @DisplayName("deleting the account of the only owner writes home.deleted too")
    void accountDeletionWritesTheMessage() {
        removeDeletedAccount.remove(OWNER);

        assertThat(messagesFor(HOME)).extracting(HomeOutboxEntity::getMessageType)
                .containsExactly(HomeMessageTypes.HOME_DELETED);
    }

    @Test
    @DisplayName("a refused deletion writes nothing")
    void refusedDeletionWritesNothing() {
        memberships.save(UserHome.member(MEMBER, HOME));

        assertThatThrownBy(() -> deleteHome.delete(new DeleteHomeCommand(MEMBER, HOME)))
                .isInstanceOf(HomeAccessDeniedException.class);

        assertThat(messagesFor(HOME)).isEmpty();
    }

    private List<HomeOutboxEntity> messagesFor(String homeId) {
        return outbox.findAll().stream()
                .filter(message -> message.getPayload().contains("\"" + homeId + "\""))
                .toList();
    }
}
