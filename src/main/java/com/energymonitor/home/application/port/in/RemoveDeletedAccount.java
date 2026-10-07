package com.energymonitor.home.application.port.in;

/**
 * Input port for dropping what this module keeps about an account that was deleted.
 */
public interface RemoveDeletedAccount {

    /**
     * Removes the account's memberships. A home the account is the only owner of is deleted with
     * all its memberships, since nobody would be left able to manage it.
     *
     * @param userId the deleted account
     */
    void remove(String userId);
}
