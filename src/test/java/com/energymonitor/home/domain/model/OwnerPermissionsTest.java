package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OwnerPermissionsTest {

    @Test
    void canManageDevicesReturnsTrue() {
        assertTrue(OwnerPermissions.INSTANCE.canManageDevices());
    }

    @Test
    void canManageHomeReturnsTrue() {
        assertTrue(OwnerPermissions.INSTANCE.canManageHome());
    }

    @Test
    void isSingleton() {
        assertTrue(OwnerPermissions.INSTANCE == OwnerPermissions.INSTANCE);
    }
}
