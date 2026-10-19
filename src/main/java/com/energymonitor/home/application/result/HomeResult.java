package com.energymonitor.home.application.result;

import com.energymonitor.home.domain.model.Home;
import java.time.Instant;

/**
 * Result of a home query: the home data without membership information.
 *
 * @param idHome       identifier
 * @param name         display name
 * @param homeTypeId   identifier of the home type
 * @param address      physical address
 * @param accessCode   unique join code
 * @param description  optional description
 * @param creationDate when the home was created
 */
public record HomeResult(String idHome, String name, String homeTypeId, String address,
                         String accessCode, String description, Instant creationDate) {

    /**
     * Creates a result from a domain object.
     *
     * @param home the domain object
     * @return the result
     */
    public static HomeResult from(Home home) {
        return new HomeResult(home.idHome(), home.name(), home.homeTypeId(), home.address(),
                home.accessCode(), home.description(), home.creationDate());
    }
}
