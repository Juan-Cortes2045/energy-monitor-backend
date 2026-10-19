package com.energymonitor.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.home.api.HomeOwnershipTransferred;
import com.energymonitor.home.application.command.TransferHomeOwnershipCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.usecase.TransferHomeOwnershipService;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransferHomeOwnershipServiceTest {

    private static final String HOME = "hom0000001";
    private static final String OWNER = "use0000001";
    private static final String MEMBER = "use0000002";

    private final HomeUseCaseFixtures.FakeHomePersistencePort homes =
            new HomeUseCaseFixtures.FakeHomePersistencePort();
    private final HomeUseCaseFixtures.FakeUserHomePersistencePort memberships =
            new HomeUseCaseFixtures.FakeUserHomePersistencePort();
    private final HomeUseCaseFixtures.FakeHomeEventPort events = new HomeUseCaseFixtures.FakeHomeEventPort();
    private final TransferHomeOwnershipService service = new TransferHomeOwnershipService(homes, memberships,
            events, Clock.fixed(HomeUseCaseFixtures.NOW, ZoneOffset.UTC));

    @BeforeEach
    void seed() {
        homes.seed(Home.create(HOME, "Casa", "hous000001", "Calle 1", "AAA11111", null,
                HomeUseCaseFixtures.NOW));
        memberships.save(UserHome.owner(OWNER, HOME));
        memberships.save(new UserHome(MEMBER, HOME, Role.MEMBER, true));
    }

    @Test
    @DisplayName("the owner hands the home over: the member becomes owner, the owner stays as a member")
    void ownerTransfersToAMember() {
        service.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, MEMBER));

        assertThat(memberships.findActive(MEMBER, HOME)).get()
                .returns(Role.OWNER, UserHome::role)
                .returns(true, UserHome::isFavorite);
        assertThat(memberships.findActive(OWNER, HOME)).get().returns(Role.MEMBER, UserHome::role);
        assertThat(memberships.countActiveOwnersByHomeId(HOME)).isEqualTo(1);
        assertThat(events.transferred).containsExactly(
                new HomeOwnershipTransferred(HOME, OWNER, MEMBER, HomeUseCaseFixtures.NOW));
    }

    @Test
    @DisplayName("the home cannot go to somebody who is not a member of it: 404 and nothing changes")
    void newOwnerMustBeAMember() {
        memberships.save(UserHome.owner("use0000009", "hom0000002"));

        assertThatThrownBy(() -> service.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, "use0000009")))
                .isInstanceOf(UserHomeNotFoundException.class);

        assertUnchanged();
    }

    @Test
    @DisplayName("a member cannot hand the home over, not even to themselves: 403 and nothing changes")
    void memberIsRefused() {
        assertThatThrownBy(() -> service.transfer(new TransferHomeOwnershipCommand(MEMBER, HOME, MEMBER)))
                .isInstanceOf(HomeAccessDeniedException.class);

        assertUnchanged();
    }

    @Test
    @DisplayName("a deleted home cannot be transferred: 404")
    void deletedHomeIsNotFound() {
        homes.remove(HOME);

        assertThatThrownBy(() -> service.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, MEMBER)))
                .isInstanceOf(HomeNotFoundException.class);

        assertUnchanged();
    }

    @Test
    @DisplayName("somebody outside the home gets 404, so the home's existence is not revealed")
    void outsiderGetsNotFound() {
        assertThatThrownBy(() -> service.transfer(new TransferHomeOwnershipCommand("use0000009", HOME, MEMBER)))
                .isInstanceOf(HomeNotFoundException.class);

        assertUnchanged();
    }

    @Test
    @DisplayName("transferring to oneself or to another owner is a conflict: 409")
    void nothingToTransfer() {
        memberships.save(UserHome.owner("use0000003", HOME));

        assertThatThrownBy(() -> service.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, OWNER)))
                .isInstanceOf(HomeConflictException.class);
        assertThatThrownBy(() -> service.transfer(new TransferHomeOwnershipCommand(OWNER, HOME, "use0000003")))
                .isInstanceOf(HomeConflictException.class);

        assertThat(memberships.findActive(OWNER, HOME)).get().returns(Role.OWNER, UserHome::role);
        assertThat(events.transferred).isEmpty();
    }

    private void assertUnchanged() {
        assertThat(memberships.findActive(OWNER, HOME)).get().returns(Role.OWNER, UserHome::role);
        assertThat(memberships.findActive(MEMBER, HOME)).get().returns(Role.MEMBER, UserHome::role);
        assertThat(events.transferred).isEmpty();
    }
}
