package com.energymonitor.home.api;

import java.util.List;
import java.util.Optional;

/**
 * Public API of the Home Management bounded context.
 *
 * <p>Other modules (e.g. {@code alert}, {@code device}) use this interface to read
 * home data without depending on the module's internals.
 */
public interface HomeApi {

    /**
     * Checks if a home exists and is active.
     *
     * @param idHome the home identifier
     * @return {@code true} when the home exists
     */
    boolean homeExists(String idHome);

    /**
     * Checks if a user is an active member of a home.
     *
     * @param idUser the user identifier
     * @param idHome the home identifier
     * @return {@code true} when the user is an active member
     */
    boolean isMember(String idUser, String idHome);

    /**
     * Checks if a user is an active OWNER of a home.
     *
     * @param idUser the user identifier
     * @param idHome the home identifier
     * @return {@code true} when the user is an active member with the OWNER role
     */
    boolean isOwner(String idUser, String idHome);

    /**
     * Identifiers of the active members of a home, owners included.
     *
     * @param idHome the home identifier
     * @return the user identifiers, empty when the home has none or does not exist
     */
    List<String> memberIds(String idHome);

    /**
     * Finds a home by identifier.
     *
     * @param idHome the home identifier
     * @return the home DTO, empty when not found
     */
    Optional<HomeDto> findHome(String idHome);

    /**
     * Finds the thresholds of a home.
     *
     * @param idHome the home identifier
     * @return the thresholds DTO, empty when not found
     */
    Optional<HomeThresholdsDto> findThresholds(String idHome);
}
