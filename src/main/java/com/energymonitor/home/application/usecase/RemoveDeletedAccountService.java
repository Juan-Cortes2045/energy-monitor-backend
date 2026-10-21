package com.energymonitor.home.application.usecase;

import com.energymonitor.home.api.HomeOwnershipTransferred;
import com.energymonitor.home.application.port.out.HomeEventPort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import java.time.Clock;
import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;

/**
 * Drops the memberships of an account the security service deleted, and the homes nobody else
 * is left in.
 *
 * <p>The deletion is already committed when this runs, so nothing here can refuse it. The
 * security service asks {@link CheckAccountDeletionService} first and does not delete the only
 * owner of a home that still has other members; should one slip through anyway (a member joined
 * between the check and the deletion), the oldest remaining member becomes the owner, so the
 * home is never left without one.
 *
 * <p>Idempotent: the account deleted message may arrive twice, and the second time there is no
 * membership left. Everything is a soft delete, like the rest of this module.
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
        for (var membership : userHomePort.listActiveByUser(userId)) {
            String homeId = membership.homeId();
            if (!isOnlyOwner(membership, homeId)) {
                userHomePort.remove(userId, homeId);
                continue;
            }
            var heir = userHomePort.listActiveByHomeId(homeId).stream()
                    .filter(other -> !other.userId().equals(userId))
                    .findFirst();
            if (heir.isPresent()) {
                userHomePort.save(heir.get().withRole(Role.OWNER));
                userHomePort.remove(userId, homeId);
                events.publish(new HomeOwnershipTransferred(homeId, userId, heir.get().userId(),
                        clock.instant()));
            } else {
                // Same end as an owner deleting it: thresholds go and its devices are unlinked.
                HomeRemoval.remove(homeId, userId, homePort, userHomePort, thresholdsPort, events, clock);
            }
        }
    }

    private boolean isOnlyOwner(UserHome membership, String homeId) {
        return membership.role() == Role.OWNER
                && userHomePort.countActiveOwnersByHomeId(homeId) == 1;
    }
}
