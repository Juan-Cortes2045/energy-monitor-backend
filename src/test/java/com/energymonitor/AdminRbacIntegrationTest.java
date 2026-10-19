package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.notification.application.usecase.AdminNotifications;
import com.energymonitor.notification.domain.model.Notification;
import com.energymonitor.security.api.AccountRegistered;
import com.energymonitor.security.api.AdministratorQuery;
import com.energymonitor.security.application.command.AssignRoleCommand;
import com.energymonitor.security.application.command.ListUserAccountsQuery;
import com.energymonitor.security.application.port.in.AssignRole;
import com.energymonitor.security.application.port.in.CheckPermission;
import com.energymonitor.security.application.port.in.ListUserAccounts;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.PersonPersistencePort;
import com.energymonitor.security.application.port.out.SystemRolePersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.application.result.UserAccountPage;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.Person;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.domain.model.UserStatus;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import com.energymonitor.security.infrastructure.JwtTokenIssuer;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The seeded ADMIN role against the real schema and the real filter chain: nobody holds a
 * global privilege until the role is granted, and once granted the administration routes open.
 *
 * <p>{@code @Transactional}: every write joins the test's transaction and is rolled back.
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class AdminRbacIntegrationTest extends JwtKeyedTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final String ADMIN = "iad0000001";
    private static final String USER = "iad0000002";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtTokenIssuer tokenIssuer;
    @Autowired
    private UserPersistencePort users;
    @Autowired
    private PersonPersistencePort persons;
    @Autowired
    private PasswordHasherPort hasher;
    @Autowired
    private SystemRolePersistencePort roles;
    @Autowired
    private AssignRole assignRole;
    @Autowired
    private CheckPermission checkPermission;
    @Autowired
    private ListUserAccounts listUserAccounts;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AdministratorQuery administratorQuery;
    @Autowired
    private AdminNotifications adminNotifications;

    private void seedUser(String idUser, String idPerson, String name, String email) {
        persons.save(new Person(idPerson, name, "Iadtest"));
        users.save(User.register(idUser, idPerson, hasher.hash("StrongPass1!"), Email.of(email),
                NOW, null));
    }

    private String bearer(String idUser, String email) {
        return "Bearer " + tokenIssuer.issue(new AuthenticatedUser(idUser, "iap" + idUser.substring(3),
                Email.of(email), UserStatus.ACTIVE, Instant.now(), null));
    }

    @BeforeEach
    void seed() {
        seedUser(ADMIN, "iap0000001", "Grace", "iad.admin@example.com");
        seedUser(USER, "iap0000002", "Ada", "iad.user@example.com");
        String adminRole = roles.findActiveByName("ADMIN").orElseThrow().idSystemRole();
        assignRole.assign(new AssignRoleCommand(ADMIN, adminRole, null));
        entityManager.flush();
    }

    @Test
    @DisplayName("the seeded ADMIN role grants the three administration codes, and only to whom it is granted")
    void theSeededRoleGrantsTheAdministrationCodes() {
        assertThat(checkPermission.listGrantedCodes(ADMIN))
                .containsExactly("ROLE.MANAGE", "USER.MANAGE", "USER.READ");
        assertThat(checkPermission.listGrantedCodes(USER)).isEmpty();
    }

    @Test
    @DisplayName("the directory searches address and name and reports each account's roles")
    void theDirectorySearchesAndReportsRoles() {
        UserAccountPage byName = listUserAccounts.list(new ListUserAccountsQuery("iadtest", 0, 10));
        UserAccountPage byEmail = listUserAccounts.list(new ListUserAccountsQuery("IAD.USER", 0, 10));
        UserAccountPage literal = listUserAccounts.list(new ListUserAccountsQuery("iad%", 0, 10));

        assertThat(byName.totalItems()).isEqualTo(2);
        assertThat(byName.items()).extracting(item -> item.idUser() + ":" + item.roles())
                .containsExactlyInAnyOrder(ADMIN + ":[ADMIN]", USER + ":[]");
        assertThat(byEmail.items()).extracting(item -> item.idUser()).containsExactly(USER);
        assertThat(literal.totalItems()).isZero();
    }

    @Test
    @DisplayName("an administrator reaches the directory and a regular user is refused")
    void onlyTheAdministratorReachesTheDirectory() throws Exception {
        mvc.perform(get("/api/v1/admin/users").param("q", "iadtest")
                        .header("Authorization", bearer(ADMIN, "iad.admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        mvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(USER, "iad.user@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("blocking an account takes away its access to the administration routes at once")
    void blockingTakesEffectOnTheNextRequest() throws Exception {
        String user = bearer(USER, "iad.user@example.com");
        mvc.perform(put("/api/v1/admin/users/" + USER + "/roles/ADMIN")
                        .header("Authorization", bearer(ADMIN, "iad.admin@example.com")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", user))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/admin/users/" + USER + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}")
                        .header("Authorization", bearer(ADMIN, "iad.admin@example.com")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/admin/users").header("Authorization", user))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an administrator cannot block their own account")
    void anAdministratorCannotBlockThemselves() throws Exception {
        mvc.perform(put("/api/v1/admin/users/" + ADMIN + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}")
                        .header("Authorization", bearer(ADMIN, "iad.admin@example.com")))
                .andExpect(status().isConflict());

        assertThat(users.findActive(ADMIN).orElseThrow().status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("only the accounts holding ADMIN are administrators, and their notifications are stored and read back")
    void administratorNotificationsAreStoredForAdministratorsOnly() {
        assertThat(administratorQuery.activeAdministratorIds()).contains(ADMIN).doesNotContain(USER);

        adminNotifications.record(new AccountRegistered(USER, "iad.user@example.com", NOW));
        entityManager.flush();

        assertThat(adminNotifications.list(ADMIN))
                .filteredOn(n -> USER.equals(n.sourceId()))
                .extracting(Notification::messageKey)
                .containsExactly("admin.accountRegistered");
        assertThat(adminNotifications.list(USER)).isEmpty();

        String id = adminNotifications.list(ADMIN).getFirst().idNotification();
        adminNotifications.markRead(ADMIN, id);
        entityManager.flush();
        entityManager.clear();
        assertThat(adminNotifications.list(ADMIN).getFirst().readAt()).isNotNull();
    }
}
