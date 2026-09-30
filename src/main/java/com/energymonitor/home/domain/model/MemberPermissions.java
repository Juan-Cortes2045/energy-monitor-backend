package com.energymonitor.home.domain.model;

/**
 * Permissions for the {@link Role#MEMBER} role.
 *
 * <p>The MEMBER has read-only access to the home.
 */
public final class MemberPermissions implements UserPermissions {

    /** Singleton instance. */
    public static final MemberPermissions INSTANCE = new MemberPermissions();

    private MemberPermissions() {
    }

    @Override
    public boolean canManageDevices() {
        return false;
    }

    @Override
    public boolean canManageHome() {
        return false;
    }
}
