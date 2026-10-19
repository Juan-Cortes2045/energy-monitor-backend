package com.energymonitor.home.adapter.in.web.dto;

/**
 * Response DTO for a home type.
 *
 * @param idHomeType identifier
 * @param name       display name
 */
public record HomeTypeResponse(
        String idHomeType,
        String name
) {
}
