package com.energymonitor.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.home.application.usecase.CheckAccountDeletionService;
import com.energymonitor.home.application.usecase.RemoveDeletedAccountService;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.home.application.exception.AccountDeletionBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RemoveDeletedAccountServiceTest {

    private final HomeUseCaseFixtures.FakeHomePersistencePort homes =
            new HomeUseCaseFixtures.FakeHomePersistencePort();
    private final HomeUseCaseFixtures.FakeUserHomePersistencePort memberships =
            new HomeUseCaseFixtures.FakeUserHomePersistencePort();
    private final HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort thresholds =
            new HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort();
    private final HomeUseCaseFixtures.FakeHomeEventPort events = new HomeUseCaseFixtures.FakeHomeEventPort();
    private final RemoveDeletedAccountService service = new RemoveDeletedAccountService(homes, memberships,
            thresholds, events, java.time.Clock.fixed(HomeUseCaseFixtures.NOW, java.time.ZoneOffset.UTC));
    private final CheckAccountDeletionService check = new CheckAccountDeletionService(memberships);

    @BeforeEach
    void seed() {
        // hom0000001: the deleted user is its only owner, with one other member.
        homes.seed(Home.create("hom0000001", "Casa", "hous000001", "Calle 1", "AAA11111", null,
                HomeUseCaseFixtures.NOW));
        memberships.save(UserHome.owner("use0000001", "hom0000001"));
        memberships.save(UserHome.member("use0000002", "hom0000001"));
        // hom0000002: the deleted user is only a member.
        homes.seed(Home.create("hom0000002", "Apto", "apar000001", "Calle 2", "BBB22222", null,
                HomeUseCaseFixtures.NOW));
        memberships.save(UserHome.owner("use0000002", "hom0000002"));
        memberships.save(UserHome.member("use0000001", "hom0000002"));
    }

    @Test
    @DisplayName("hands a home it owned alone to the oldest member left, instead of refusing")
    void transfersToTheOldestMember() {
        memberships.save(UserHome.member("use0000003", "hom0000001"));

        service.remove("use0000001");

        assertThat(homes.findActive("hom0000001")).isPresent();
        assertThat(memberships.findActive("use0000002", "hom0000001"))
                .map(UserHome::role).contains(Role.OWNER);
        assertThat(memberships.findActive("use0000003", "hom0000001"))
                .map(UserHome::role).contains(Role.MEMBER);
        assertThat(memberships.listActiveByUser("use0000001")).isEmpty();
        assertThat(events.transferred).singleElement().satisfies(transfer -> {
            assertThat(transfer.homeId()).isEqualTo("hom0000001");
            assertThat(transfer.previousOwnerId()).isEqualTo("use0000001");
            assertThat(transfer.newOwnerId()).isEqualTo("use0000002");
        });
        assertThat(events.deleted).isEmpty();
    }

    @Test
    @DisplayName("a second delivery of the same deletion changes nothing")
    void isIdempotent() {
        service.remove("use0000001");
        service.remove("use0000001");

        assertThat(events.transferred).hasSize(1);
        assertThat(memberships.listActiveByHomeId("hom0000001"))
                .extracting(UserHome::userId).containsExactly("use0000002");
    }

    @Test
    @DisplayName("deletes a home the user is the only member of, and leaves the others standing")
    void deletesEmptyHomesAndKeepsTheOthers() {
        memberships.remove("use0000002", "hom0000001");

        service.remove("use0000001");

        assertThat(homes.findActive("hom0000001")).isEmpty();
        assertThat(memberships.listActiveByHomeId("hom0000001")).isEmpty();
        assertThat(homes.findActive("hom0000002")).isPresent();
        assertThat(memberships.listActiveByHomeId("hom0000002"))
                .extracting(UserHome::userId).containsExactly("use0000002");
        assertThat(memberships.listActiveByUser("use0000001")).isEmpty();
        // Same propagation as an owner deleting it: the device module unlinks its modules.
        assertThat(events.deleted).extracting(com.energymonitor.home.api.HomeDeleted::homeId)
                .containsExactly("hom0000001");
    }

    @Test
    @DisplayName("tells the security service up front that the deletion is refused, and changes nothing")
    void checkRefusesSoleOwnerOfAPopulatedHome() {
        assertThatThrownBy(() -> check.ensureDeletable("use0000001"))
                .isInstanceOf(AccountDeletionBlockedException.class)
                .hasMessageContaining("only owner");

        assertThat(memberships.listActiveByUser("use0000001")).hasSize(2);
    }

    @Test
    @DisplayName("lets the deletion go once nobody else is in the homes the user owns alone")
    void checkAllowsOtherwise() {
        memberships.remove("use0000002", "hom0000001");

        check.ensureDeletable("use0000001");

        assertThat(memberships.listActiveByUser("use0000001")).hasSize(2);
    }
}
