package com.energymonitor.home.adapter.in.web.dto;

import java.time.Instant;

/**
 * Response DTO for a home.
 *
 * @param idHome       identifier
 * @param name         display name
 * @param homeTypeId   identifier of the home type
 * @param address      physical address
 * @param accessCode   unique join code
 * @param description  optional description
 * @param creationDate when the home was created
 */
public record HomeResponse(
        String idHome,
        String name,
        String homeTypeId,
        String address,
        String accessCode,
        String description,
        Instant creationDate
) {
}
