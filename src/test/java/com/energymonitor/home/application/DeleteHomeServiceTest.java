package com.energymonitor.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.usecase.DeleteHomeService;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeleteHomeServiceTest {

    private final HomeUseCaseFixtures.FakeHomePersistencePort homes =
            new HomeUseCaseFixtures.FakeHomePersistencePort();
    private final HomeUseCaseFixtures.FakeUserHomePersistencePort memberships =
            new HomeUseCaseFixtures.FakeUserHomePersistencePort();
    private final HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort thresholds =
            new HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort();
    private final HomeUseCaseFixtures.FakeHomeEventPort events = new HomeUseCaseFixtures.FakeHomeEventPort();
    private final DeleteHomeService service = new DeleteHomeService(homes, memberships, thresholds, events,
            Clock.fixed(HomeUseCaseFixtures.NOW, ZoneOffset.UTC));

    @BeforeEach
    void seed() {
        homes.seed(Home.create("hom0000001", "Casa", "hous000001", "Calle 1", "AAA11111", null,
                HomeUseCaseFixtures.NOW));
        thresholds.save(HomeThresholds.create("thr0000001", "hom0000001", 6.0, 180.0, true));
        memberships.save(UserHome.owner("use0000001", "hom0000001"));
        memberships.save(UserHome.member("use0000002", "hom0000001"));
    }

    @Test
    @DisplayName("an owner deletes the home, its memberships and thresholds, and announces it")
    void ownerDeletesTheHome() {
        service.delete(new DeleteHomeCommand("use0000001", "hom0000001"));

        assertThat(homes.findActive("hom0000001")).isEmpty();
        assertThat(memberships.listActiveByHomeId("hom0000001")).isEmpty();
        assertThat(thresholds.findActiveByHomeId("hom0000001")).isEmpty();
        assertThat(events.deleted).containsExactly(
                new HomeDeleted("hom0000001", "use0000001", HomeUseCaseFixtures.NOW));
    }

    @Test
    @DisplayName("a member cannot delete the home: 403 and nothing changes")
    void memberIsRefused() {
        assertThatThrownBy(() -> service.delete(new DeleteHomeCommand("use0000002", "hom0000001")))
                .isInstanceOf(HomeAccessDeniedException.class);

        assertThat(homes.findActive("hom0000001")).isPresent();
        assertThat(memberships.listActiveByHomeId("hom0000001")).hasSize(2);
        assertThat(thresholds.findActiveByHomeId("hom0000001")).isPresent();
        assertThat(events.deleted).isEmpty();
    }

    @Test
    @DisplayName("somebody outside the home gets 404, so the home's existence is not revealed")
    void outsiderGetsNotFound() {
        assertThatThrownBy(() -> service.delete(new DeleteHomeCommand("use0000009", "hom0000001")))
                .isInstanceOf(HomeNotFoundException.class);
        assertThatThrownBy(() -> service.delete(new DeleteHomeCommand("use0000001", "hom0000404")))
                .isInstanceOf(HomeNotFoundException.class);

        assertThat(events.deleted).isEmpty();
    }

    @Test
    @DisplayName("any owner of a home with several owners can delete it")
    void anyOwnerCanDelete() {
        memberships.save(UserHome.owner("use0000003", "hom0000001"));

        service.delete(new DeleteHomeCommand("use0000003", "hom0000001"));

        assertThat(homes.findActive("hom0000001")).isEmpty();
    }
}
