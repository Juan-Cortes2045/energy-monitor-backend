package com.energymonitor.home.application.port.in;

import com.energymonitor.security.api.AccountDeletionBlockedException;

/**
 * Input port for the security service asking, before it deletes an account, whether this module
 * lets it go.
 */
public interface CheckAccountDeletion {

    /**
     * Returns normally when the account may be deleted.
     *
     * @param userId the account about to be deleted
     * @throws AccountDeletionBlockedException when it is the only owner of a home that still has
     *                                         other members, with a message for the user
     */
    void ensureDeletable(String userId);
}
