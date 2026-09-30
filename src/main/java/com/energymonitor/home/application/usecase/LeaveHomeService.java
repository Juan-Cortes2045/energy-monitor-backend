package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.LeaveHomeCommand;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.LastOwnerCannotLeaveException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.port.in.LeaveHome;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.UserHome;

/**
 * The user leaves a home.
 *
 * <p>HOME-INV-002: the only OWNER cannot leave. The service checks the owner count
 * before allowing the leave operation.
 */
public class LeaveHomeService implements LeaveHome {

    private final UserHomePersistencePort userHomePort;

    public LeaveHomeService(UserHomePersistencePort userHomePort) {
        this.userHomePort = userHomePort;
    }

    @Override
    public void leave(LeaveHomeCommand command) {
        // Verify membership
        UserHome membership = userHomePort.findActive(command.userId(), command.homeId())
                .orElseThrow(() -> new UserHomeNotFoundException(
                        "no active membership for user " + command.userId() + " in home " + command.homeId()));

        // Check if user is the only OWNER
        boolean otherOwnersExist = userHomePort.countActiveOwnersByHomeId(command.homeId()) > 1;

        // Domain validates HOME-INV-002
        try {
            membership.leaveHome(otherOwnersExist);
        } catch (IllegalStateException e) {
            throw new LastOwnerCannotLeaveException(e.getMessage());
        }

        // Soft delete
        userHomePort.remove(command.userId(), command.homeId());
    }
}
