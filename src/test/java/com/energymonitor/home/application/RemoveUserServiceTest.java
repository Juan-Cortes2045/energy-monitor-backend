package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.home.application.command.RemoveUserCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.usecase.RemoveUserService;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveUserServiceTest {

    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private RemoveUserService service;

    @BeforeEach
    void setUp() {
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();

        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));
        userHomePort.save(UserHome.member("use0000002", "hom0000001"));

        service = new RemoveUserService(userHomePort);
    }

    @Test
    void removeUserWithOwnerPermission() {
        var command = new RemoveUserCommand("use0000001", "hom0000001", "use0000002");
        service.remove(command);

        // Verify removal
        assert userHomePort.findActive("use0000002", "hom0000001").isEmpty();
    }

    @Test
    void removeUserWithoutMembershipThrows404() {
        var command = new RemoveUserCommand("use0000003", "hom0000001", "use0000002");
        assertThrows(HomeNotFoundException.class, () -> service.remove(command));
    }

    @Test
    void removeUserAsMemberWithoutPermissionThrows403() {
        var command = new RemoveUserCommand("use0000002", "hom0000001", "use0000001");
        assertThrows(HomeAccessDeniedException.class, () -> service.remove(command));
    }

    @Test
    void removeNonExistentTargetThrows() {
        var command = new RemoveUserCommand("use0000001", "hom0000001", "use0000003");
        assertThrows(UserHomeNotFoundException.class, () -> service.remove(command));
    }

    @Test
    void removeOwnerThrowsConflict() {
        var command = new RemoveUserCommand("use0000001", "hom0000001", "use0000001");
        assertThrows(HomeConflictException.class, () -> service.remove(command));
    }
}
