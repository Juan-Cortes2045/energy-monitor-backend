package com.energymonitor.home.application.command;

/**
 * Input of {@code GetHomeType}: retrieves a home type by identifier.
 *
 * @param idHomeType the identifier of the home type
 */
public record GetHomeTypeQuery(String idHomeType) {
}
