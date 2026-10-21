package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;

import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the home module drops when the security service reports an account deleted, against the
 * real schema.
 *
 * <p>{@code @Transactional}: the cleanup joins the test's transaction, which is rolled back, so
 * nothing written here outlives the test.
 */
@SpringBootTest
@Transactional
class AccountDeletedCleanupIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Autowired
    private RemoveDeletedAccount removeDeletedAccount;
    @Autowired
    private HomePersistencePort homes;
    @Autowired
    private UserHomePersistencePort memberships;

    @Test
    @DisplayName("a home the account was alone in goes, and the others keep their members")
    void deletesTheHomesItWasAloneIn() {
        homes.save(Home.create("adh0000001", "Casa", "hous000001", "Calle 1", "ADH00001", null, NOW));
        memberships.save(UserHome.owner("adu0000001", "adh0000001"));
        homes.save(Home.create("adh0000002", "Apto", "apar000001", "Calle 2", "ADH00002", null, NOW));
        memberships.save(UserHome.owner("adu0000002", "adh0000002"));
        memberships.save(UserHome.member("adu0000001", "adh0000002"));

        removeDeletedAccount.remove("adu0000001");

        assertThat(homes.findActive("adh0000001")).isEmpty();
        assertThat(memberships.listActiveByHomeId("adh0000001")).isEmpty();
        assertThat(homes.findActive("adh0000002")).isPresent();
        assertThat(memberships.listActiveByHomeId("adh0000002"))
                .extracting(UserHome::userId).containsExactly("adu0000002");
    }

    @Test
    @DisplayName("a home it owned alone with other members passes to the oldest of them")
    void transfersTheHomeItOwnedAlone() {
        homes.save(Home.create("adh0000003", "Casa", "hous000001", "Calle 3", "ADH00003", null, NOW));
        memberships.save(UserHome.owner("adu0000004", "adh0000003"));
        // adu0000005 joins first; within the same second the identifier keeps it first too.
        memberships.save(UserHome.member("adu0000005", "adh0000003"));
        memberships.save(UserHome.member("adu0000006", "adh0000003"));

        removeDeletedAccount.remove("adu0000004");

        assertThat(homes.findActive("adh0000003")).isPresent();
        assertThat(memberships.findActive("adu0000005", "adh0000003"))
                .map(UserHome::role).contains(Role.OWNER);
        assertThat(memberships.findActive("adu0000006", "adh0000003"))
                .map(UserHome::role).contains(Role.MEMBER);
        assertThat(memberships.findActive("adu0000004", "adh0000003")).isEmpty();
    }
}
