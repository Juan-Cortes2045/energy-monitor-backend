package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.port.in.CheckAccountDeletion;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.security.api.AccountDeletionBlockedException;
import java.util.List;

/**
 * Refuses the deletion of an account that is the only owner of a home still having other members:
 * nobody would be left able to manage that home. Asked by the security service before it deletes
 * the account, so the refusal reaches the user before anything is deleted.
 */
public class CheckAccountDeletionService implements CheckAccountDeletion {

    private final UserHomePersistencePort userHomePort;

    public CheckAccountDeletionService(UserHomePersistencePort userHomePort) {
        this.userHomePort = userHomePort;
    }

    @Override
    public void ensureDeletable(String userId) {
        ensureDeletable(userHomePort.listActiveByUser(userId), userHomePort);
    }

    /**
     * The rule, shared with {@link RemoveDeletedAccountService}, which checks it again when the
     * deletion arrives.
     *
     * @param memberships  the active memberships of the account
     * @param userHomePort where the other members are read
     */
    static void ensureDeletable(List<UserHome> memberships, UserHomePersistencePort userHomePort) {
        for (var membership : memberships) {
            String homeId = membership.homeId();
            if (membership.role() == Role.OWNER
                    && userHomePort.countActiveOwnersByHomeId(homeId) == 1
                    && userHomePort.listActiveByHomeId(homeId).size() > 1) {
                throw new AccountDeletionBlockedException("You are the only owner of a home that "
                        + "still has other members. Transfer the ownership or remove the members "
                        + "before deleting your account.");
            }
        }
    }
}
