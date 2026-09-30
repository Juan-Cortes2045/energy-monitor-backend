package com.energymonitor.home.application.port.out;

import com.energymonitor.home.domain.model.UserHome;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting and querying user-home memberships.
 *
 * <p>The {@code save} method implements the <em>reactivation</em> strategy: when a row
 * with the same composite key {@code (userId, homeId)} exists but is soft-deleted, the
 * adapter clears {@code deleted_at} and revives it instead of inserting a duplicate.
 * The service guarantees that a reactivated membership is always MEMBER with
 * {@code favorite = false}.
 */
public interface UserHomePersistencePort {

    /**
     * Saves a membership. If a soft-deleted row with the same key exists, it is reactivated.
     *
     * @param membership the domain object
     * @return the same domain object
     */
    UserHome save(UserHome membership);

    /**
     * Soft-deletes a membership.
     *
     * @param userId the user
     * @param homeId the home
     */
    void remove(String userId, String homeId);

    /**
     * Finds an active membership by user and home.
     *
     * @param userId the user
     * @param homeId the home
     * @return the membership, empty when soft-deleted or missing
     */
    Optional<UserHome> findActive(String userId, String homeId);

    /**
     * Lists all active memberships of a user.
     *
     * @param userId the user
     * @return the memberships
     */
    List<UserHome> listActiveByUser(String userId);

    /**
     * Lists all active memberships of a home.
     *
     * @param homeId the home
     * @return the memberships
     */
    List<UserHome> listActiveByHomeId(String homeId);

    /**
     * Counts the active OWNER memberships of a home.
     *
     * @param homeId the home
     * @return the number of active owners
     */
    long countActiveOwnersByHomeId(String homeId);
}
