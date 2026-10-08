package com.energymonitor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.PersonPersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.Person;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.domain.model.UserStatus;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import com.energymonitor.security.infrastructure.JwtTokenIssuer;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Who a home request is made as, through the production filter chain and a real RS256 token.
 *
 * <p>The security module issues the token and the home module reads its subject, so this crosses
 * both modules and lives at the root like {@link AccountDeletionIntegrationTest}.
 *
 * <p><strong>Why MockMvc is built by hand.</strong> {@code @AutoConfigureMockMvc} or a
 * {@code @MockitoBean} would give this class its own application context, and every cached context
 * keeps its own connection pool open against the one MySQL the suite shares. One more context was
 * enough to exhaust {@code max_connections}. A plain {@code @SpringBootTest} reuses the context the
 * persistence tests already started, and the filter chain is applied with {@code springSecurity()}.
 *
 * <p>{@code @Transactional}: MockMvc runs in the test's thread, so the requests see the seeded
 * accounts and homes, and everything is rolled back afterwards.
 */
@SpringBootTest
@Transactional
class HomeCallerIdentityIntegrationTest extends JwtKeyedTest {

    private static final String PASSWORD = "StrongPass1!";
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final String ALICE = "hci0000001";
    private static final String BOB = "hci0000002";
    private static final String ALICE_HOME = "hch0000001";
    private static final String BOB_HOME = "hch0000002";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenIssuer tokenIssuer;

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

    private MockMvc mvc;

    @BeforeEach
    void seedTwoAccountsEachOwningOneHome() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        seedOwner(ALICE, "hcp0000001", "hci.alice@example.com", ALICE_HOME, "HCI00001");
        seedOwner(BOB, "hcp0000002", "hci.bob@example.com", BOB_HOME, "HCI00002");
    }

    private void seedOwner(String idUser, String idPerson, String email, String idHome,
                           String accessCode) {
        persons.save(new Person(idPerson, "Ada", "Lovelace"));
        users.save(User.register(idUser, idPerson, hasher.hash(PASSWORD), Email.of(email), NOW,
                null));
        homes.save(Home.create(idHome, "Casa", "hous000001", "Calle 1", accessCode, null, NOW));
        memberships.save(UserHome.owner(idUser, idHome));
    }

    private String bearer(String idUser) {
        return "Bearer " + tokenIssuer.issue(new AuthenticatedUser(idUser, "hcp0000001",
                Email.of(idUser + "@example.com"), UserStatus.ACTIVE, Instant.now(), null));
    }

    @Test
    void listsTheHomesOfTheTokenSubject() throws Exception {
        mvc.perform(get("/api/v1/homes").header("Authorization", bearer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idHome").value(ALICE_HOME));
    }

    @Test
    void ignoresAnXUserIdHeaderThatTriesToActAsSomeoneElse() throws Exception {
        // The attack the header made possible: a valid token for Alice and a header naming Bob.
        mvc.perform(get("/api/v1/homes")
                        .header("Authorization", bearer(ALICE))
                        .header("X-User-Id", BOB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idHome").value(ALICE_HOME));
    }

    @Test
    void refusesARequestWithoutAToken() throws Exception {
        mvc.perform(get("/api/v1/homes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesARequestThatOnlyNamesAUserInTheHeader() throws Exception {
        mvc.perform(get("/api/v1/homes").header("X-User-Id", ALICE))
                .andExpect(status().isUnauthorized());
    }
}
