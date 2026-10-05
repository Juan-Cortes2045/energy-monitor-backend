package com.energymonitor.home.domain.model;

/**
 * Permissions granted to a user inside a home.
 *
 * <p>Strategy pattern: the concrete implementation depends on the {@link Role}.
 */
public interface UserPermissions {

    /**
     * Whether the user can manage devices in the home.
     *
     * @return {@code true} when device management is allowed
     */
    boolean canManageDevices();

    /**
     * Whether the user can manage the home itself (members, thresholds, etc.).
     *
     * @return {@code true} when home management is allowed
     */
    boolean canManageHome();
}
