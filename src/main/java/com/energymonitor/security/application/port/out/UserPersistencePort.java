package com.energymonitor.security.application.port.out;

import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.User;
import java.util.Optional;

/**
 * Output port for persisting {@link User}.
 */
public interface UserPersistencePort {

    /**
     * Inserts the user, updates an existing one, or reactivates a soft-deleted row.
     *
     * @param user the domain object
     * @return the same domain object
     */
    User save(User user);

    /**
     * Finds the active user by identifier.
     *
     * @param idUser the identifier
     * @return the domain object, empty when soft-deleted or missing
     */
    Optional<User> findActive(String idUser);

    /**
     * Finds the active user by normalised email.
     *
     * @param email the address
     * @return the domain object, empty when soft-deleted or unknown
     */
    Optional<User> findActiveByEmail(Email email);
}