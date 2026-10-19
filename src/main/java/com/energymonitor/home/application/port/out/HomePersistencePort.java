package com.energymonitor.home.application.port.out;

import com.energymonitor.home.domain.model.Home;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting and querying homes.
 */
public interface HomePersistencePort {

    /**
     * Saves a home (insert or update).
     *
     * @param home the domain object
     * @return the same domain object
     */
    Home save(Home home);

    /**
     * Finds an active home by identifier.
     *
     * @param idHome the identifier
     * @return the home, empty when soft-deleted or missing
     */
    Optional<Home> findActive(String idHome);

    /**
     * Finds an active home by access code.
     *
     * @param accessCode the join code
     * @return the home, empty when soft-deleted or missing
     */
    Optional<Home> findActiveByAccessCode(String accessCode);

    /**
     * Whether an active home exists with the given access code.
     *
     * @param accessCode the join code
     * @return {@code true} when a home with that code exists
     */
    boolean existsActiveByAccessCode(String accessCode);

    /**
     * Finds active homes by a collection of identifiers (batch query, avoids N+1).
     *
     * @param ids the identifiers
     * @return the active homes
     */
    List<Home> findActiveByIds(Collection<String> ids);

    /**
     * Soft-deletes a home. Its memberships are not touched here.
     *
     * @param idHome the identifier
     */
    void remove(String idHome);
}
