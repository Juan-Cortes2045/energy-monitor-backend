package com.energymonitor.home.application.result;

import com.energymonitor.home.domain.model.HomeType;

/**
 * Result of a home type query.
 *
 * @param idHomeType identifier
 * @param name       display name
 */
public record HomeTypeResult(String idHomeType, String name) {

    /**
     * Creates a result from a domain object.
     *
     * @param type the domain object
     * @return the result
     */
    public static HomeTypeResult from(HomeType type) {
        return new HomeTypeResult(type.idHomeType(), type.name());
    }
}
