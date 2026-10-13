package com.energymonitor.home.application.usecase;

import com.energymonitor.home.api.HomeOwnershipTransferred;
import com.energymonitor.home.application.command.TransferHomeOwnershipCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.port.in.TransferHomeOwnership;
import com.energymonitor.home.application.port.out.HomeEventPort;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Clock;

/**
 * The owner of a home hands it over to one of its members.
 *
 * <p><strong>The previous owner stays, as a MEMBER.</strong> The membership model has two roles
 * and no third state, and leaving is a separate decision the user can still take afterwards with
 * {@code LeaveHome}. Removing them here would make the transfer also an exit nobody asked for.
 * The roles are swapped in one step (HOME-INV-015), so the home keeps exactly as many owners as
 * it had: never two, never none (HOME-INV-001).
 *
 * <p><strong>Who.</strong> Only an OWNER, and only towards an active member of the same home:
 * <ul>
 *   <li>caller not in the home, or the home deleted (its memberships are gone with it): 404,
 *       as for every other home endpoint, so the endpoint does not reveal which homes exist;</li>
 *   <li>caller is a MEMBER: 403;</li>
 *   <li>the new owner is not an active member: 404 ({@link UserHomeNotFoundException});</li>
 *   <li>the new owner is the caller or already an owner: 409, there is nothing to transfer.</li>
 * </ul>
 *
 * <p><strong>What does not change.</strong> Thresholds, devices and measurements belong to the
 * home, not to its owner, so they are not touched. Each user keeps their favourite mark.
 *
 * <p>The two memberships are read with a row lock, the caller's first, so two transfers of the
 * same home at the same time run one after the other and the second sees a MEMBER. The writes and
 * the event run in one transaction (see {@code TransactionalTransferHomeOwnership}). This service
 * contains no Spring annotations.
 */
public class TransferHomeOwnershipService implements TransferHomeOwnership {

    private final HomePersistencePort homePort;
    private final UserHomePersistencePort userHomePort;
    private final HomeEventPort events;
    private final Clock clock;

    public TransferHomeOwnershipService(HomePersistencePort homePort, UserHomePersistencePort userHomePort,
                                        HomeEventPort events, Clock clock) {
        this.homePort = homePort;
        this.userHomePort = userHomePort;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void transfer(TransferHomeOwnershipCommand command) {
        String homeId = command.homeId();
        UserHome owner = userHomePort.lockActive(command.userId(), homeId)
                .orElseThrow(() -> new HomeNotFoundException(
                        "no active membership for user " + command.userId() + " in home " + homeId));
        if (homePort.findActive(homeId).isEmpty()) {
            throw new HomeNotFoundException("home " + homeId + " does not exist");
        }
        if (!owner.isOwner()) {
            throw new HomeAccessDeniedException("only the owner can transfer home " + homeId);
        }
        if (owner.userId().equals(command.newOwnerId())) {
            throw new HomeConflictException("user " + command.userId() + " already owns home " + homeId);
        }
        UserHome newOwner = userHomePort.lockActive(command.newOwnerId(), homeId)
                .orElseThrow(() -> new UserHomeNotFoundException(
                        "user " + command.newOwnerId() + " is not a member of home " + homeId));
        if (newOwner.isOwner()) {
            throw new HomeConflictException("user " + command.newOwnerId() + " already owns home " + homeId);
        }

        userHomePort.save(owner.withRole(Role.MEMBER));
        userHomePort.save(newOwner.withRole(Role.OWNER));
        events.publish(new HomeOwnershipTransferred(homeId, owner.userId(), newOwner.userId(),
                clock.instant()));
    }
}
