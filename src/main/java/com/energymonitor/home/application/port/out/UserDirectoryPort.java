package com.energymonitor.home.application.port.out;

import java.util.Collection;
import java.util.Map;

/**
 * Output port for showing who the users of a home are. This module stores user identifiers only;
 * names and addresses belong to the account owner's context.
 */
public interface UserDirectoryPort {

    /**
     * @param userIds user identifiers
     * @return the people found, keyed by user identifier; unknown identifiers are absent
     */
    Map<String, UserSummary> findByIds(Collection<String> userIds);

    /** What the home screens show about a user. */
    record UserSummary(String name, String lastName, String email) {
    }
}
