package com.energymonitor.home.application.port.out;

import com.energymonitor.home.domain.model.HomeType;
import java.util.List;
import java.util.Optional;

/**
 * Output port for querying home types (read-only catalog).
 */
public interface HomeTypePersistencePort {

    /**
     * Finds an active home type by identifier.
     *
     * @param idHomeType the identifier
     * @return the home type, empty when soft-deleted or missing
     */
    Optional<HomeType> findActive(String idHomeType);

    /**
     * Lists all active home types.
     *
     * @return the home types
     */
    List<HomeType> findAllActive();
}
