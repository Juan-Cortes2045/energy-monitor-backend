package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.home.application.command.LeaveHomeCommand;
import com.energymonitor.home.application.exception.LastOwnerCannotLeaveException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.usecase.LeaveHomeService;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaveHomeServiceTest {

    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private LeaveHomeService service;

    @BeforeEach
    void setUp() {
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();
        service = new LeaveHomeService(userHomePort);
    }

    @Test
    void leaveHomeAsMember() {
        userHomePort.save(UserHome.member("use0000001", "hom0000001"));

        var command = new LeaveHomeCommand("use0000001", "hom0000001");
        service.leave(command);

        assert userHomePort.findActive("use0000001", "hom0000001").isEmpty();
    }

    @Test
    void leaveHomeAsOwnerWithOtherOwners() {
        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));
        userHomePort.save(UserHome.owner("use0000002", "hom0000001"));

        var command = new LeaveHomeCommand("use0000001", "hom0000001");
        service.leave(command);

        assert userHomePort.findActive("use0000001", "hom0000001").isEmpty();
    }

    @Test
    void leaveHomeAsOnlyOwnerThrows() {
        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));

        var command = new LeaveHomeCommand("use0000001", "hom0000001");
        assertThrows(LastOwnerCannotLeaveException.class, () -> service.leave(command));
    }

    @Test
    void leaveHomeWithoutMembershipThrows() {
        var command = new LeaveHomeCommand("use0000001", "hom0000001");
        assertThrows(UserHomeNotFoundException.class, () -> service.leave(command));
    }
}
