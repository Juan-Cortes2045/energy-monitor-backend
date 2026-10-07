package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.api.AccountDeletionBlockedException;
import com.energymonitor.security.application.command.DeleteAccountCommand;
import com.energymonitor.security.application.command.RegisterUserCommand;
import com.energymonitor.security.application.port.in.DeleteAccount;
import com.energymonitor.security.application.port.in.RegisterUser;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.PersonPersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.Person;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account deletion across modules, against the real schema: the security module deletes the
 * account and the home module, listening to the event, drops its homes and memberships.
 *
 * <p>{@code @Transactional}: the deletion joins the test's transaction, which is rolled back, so
 * nothing written here outlives the test.
 */
@SpringBootTest
@Transactional
class AccountDeletionIntegrationTest extends JwtKeyedTest {

    private static final String PASSWORD = "StrongPass1!";
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Autowired
    private DeleteAccount deleteAccount;
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
    private RegisterUser registerUser;
    @Autowired
    private EntityManager entityManager;

    private void seedUser(String idUser, String idPerson, String email) {
        persons.save(new Person(idPerson, "Ada", "Lovelace"));
        users.save(User.register(idUser, idPerson, hasher.hash(PASSWORD), Email.of(email), NOW, null));
    }

    @Test
    @DisplayName("deleting an account deletes its home when it is alone in it and keeps the others")
    void deletesTheAccountAndItsHomes() {
        seedUser("itd0000001", "itp0000001", "itd.deleted@example.com");
        seedUser("itd0000002", "itp0000002", "itd.other@example.com");
        homes.save(Home.create("ith0000001", "Casa", "hous000001", "Calle 1", "ITH00001", null, NOW));
        memberships.save(UserHome.owner("itd0000001", "ith0000001"));
        homes.save(Home.create("ith0000002", "Apto", "apar000001", "Calle 2", "ITH00002", null, NOW));
        memberships.save(UserHome.owner("itd0000002", "ith0000002"));
        memberships.save(UserHome.member("itd0000001", "ith0000002"));

        deleteAccount.delete(new DeleteAccountCommand("itd0000001", PASSWORD, "127.0.0.1"));

        assertThat(users.findActive("itd0000001")).isEmpty();
        assertThat(persons.findActive("itp0000001")).isEmpty();
        assertThat(homes.findActive("ith0000001")).isEmpty();
        assertThat(memberships.listActiveByHomeId("ith0000001")).isEmpty();
        assertThat(homes.findActive("ith0000002")).isPresent();
        assertThat(memberships.listActiveByHomeId("ith0000002"))
                .extracting(UserHome::userId).containsExactly("itd0000002");
        assertThat(users.findActive("itd0000002")).isPresent();
    }

    @Test
    @DisplayName("deleting is refused while the account is the only owner of a home with members")
    void refusedForSoleOwnerOfPopulatedHome() {
        seedUser("itd0000004", "itp0000004", "itd.owner@example.com");
        seedUser("itd0000005", "itp0000005", "itd.member@example.com");
        homes.save(Home.create("ith0000003", "Casa", "hous000001", "Calle 3", "ITH00003", null, NOW));
        memberships.save(UserHome.owner("itd0000004", "ith0000003"));
        memberships.save(UserHome.member("itd0000005", "ith0000003"));

        assertThatThrownBy(() -> deleteAccount.delete(
                new DeleteAccountCommand("itd0000004", PASSWORD, "127.0.0.1")))
                .isInstanceOf(AccountDeletionBlockedException.class);
    }

    @Test
    @DisplayName("a deleted account releases its address, so the address can register again")
    void releasesTheAddress() {
        seedUser("itd0000003", "itp0000003", "itd.again@example.com");

        deleteAccount.delete(new DeleteAccountCommand("itd0000003", PASSWORD, "127.0.0.1"));
        // Written before the registration, as two separate requests would be.
        entityManager.flush();

        Object kept = entityManager.createNativeQuery(
                        "SELECT email FROM user WHERE id_user = 'itd0000003'")
                .getSingleResult();
        assertThat(kept).isEqualTo("deleted+itd0000003@deleted.invalid");

        User again = registerUser.register(new RegisterUserCommand("itd.again@example.com",
                PASSWORD, "Ada", "Lovelace", null, "127.0.0.1"));
        entityManager.flush();
        assertThat(again.idUser()).isNotEqualTo("itd0000003");
        assertThat(users.findActiveByEmail(Email.of("itd.again@example.com")))
                .map(User::idUser).contains(again.idUser());
    }
}
