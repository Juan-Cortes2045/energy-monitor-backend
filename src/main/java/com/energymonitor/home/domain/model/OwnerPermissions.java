package com.energymonitor.home.domain.model;

/**
 * Permissions for the {@link Role#OWNER} role.
 *
 * <p>The OWNER has full control of the home.
 */
public final class OwnerPermissions implements UserPermissions {

    /** Singleton instance. */
    public static final OwnerPermissions INSTANCE = new OwnerPermissions();

    private OwnerPermissions() {
    }

    @Override
    public boolean canManageDevices() {
        return true;
    }

    @Override
    public boolean canManageHome() {
        return true;
    }
}
