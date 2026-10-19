package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.port.out.HomeEventPort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import java.time.Clock;
import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.api.AccountDeletionBlockedException;

/**
 * Drops the memberships of a deleted account, and the homes it was the only owner of, as long as
 * nobody else is left in them. A sole owner of a home that still has other members cannot delete
 * the account until it hands the ownership over or the members leave.
 *
 * <p>Runs inside the account deletion's transaction (see {@code AccountDeletedListener}), so a
 * failure here rolls the deletion back instead of leaving homes without an owner. Everything is a
 * soft delete, like the rest of this module.
 */
public class RemoveDeletedAccountService implements RemoveDeletedAccount {

    private final HomePersistencePort homePort;
    private final UserHomePersistencePort userHomePort;
    private final HomeThresholdsPersistencePort thresholdsPort;
    private final HomeEventPort events;
    private final Clock clock;

    public RemoveDeletedAccountService(HomePersistencePort homePort,
                                       UserHomePersistencePort userHomePort,
                                       HomeThresholdsPersistencePort thresholdsPort, HomeEventPort events,
                                       Clock clock) {
        this.homePort = homePort;
        this.userHomePort = userHomePort;
        this.thresholdsPort = thresholdsPort;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void remove(String userId) {
        var memberships = userHomePort.listActiveByUser(userId);
        // Checked up front so nothing is dropped when the deletion is going to be refused.
        for (var membership : memberships) {
            if (isOnlyOwner(membership, membership.homeId())
                    && userHomePort.listActiveByHomeId(membership.homeId()).size() > 1) {
                throw new AccountDeletionBlockedException("You are the only owner of a home that "
                        + "still has other members. Transfer the ownership or remove the members "
                        + "before deleting your account.");
            }
        }
        for (var membership : memberships) {
            String homeId = membership.homeId();
            if (isOnlyOwner(membership, homeId)) {
                // Same end as an owner deleting it: thresholds go and its devices are unlinked.
                HomeRemoval.remove(homeId, userId, homePort, userHomePort, thresholdsPort, events, clock);
            } else {
                userHomePort.remove(userId, homeId);
            }
        }
    }

    private boolean isOnlyOwner(UserHome membership, String homeId) {
        return membership.role() == Role.OWNER
                && userHomePort.countActiveOwnersByHomeId(homeId) == 1;
    }
}
