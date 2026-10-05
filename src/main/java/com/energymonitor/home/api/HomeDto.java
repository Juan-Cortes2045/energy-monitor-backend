package com.energymonitor.home.api;

import java.time.Instant;

/**
 * DTO for exposing home data to other modules.
 *
 * @param idHome       identifier
 * @param name         display name
 * @param homeTypeId   identifier of the home type
 * @param address      physical address
 * @param creationDate when the home was created
 */
public record HomeDto(
        String idHome,
        String name,
        String homeTypeId,
        String address,
        Instant creationDate
) {
}
