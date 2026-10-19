package com.energymonitor.home.domain.model;

/**
 * Role of a user inside a home.
 *
 * <p>Home roles are different from global system roles: they grant permissions
 * within a specific home, not across the whole system.
 *
 * <ul>
 *   <li>{@link #OWNER} — full control of the home (manage devices, manage members, update thresholds).</li>
 *   <li>{@link #MEMBER} — read-only access to the home.</li>
 * </ul>
 */
public enum Role {

    /** Full control of the home. */
    OWNER,

    /** Read-only access to the home. */
    MEMBER
}
