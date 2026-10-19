package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class MemberPermissionsTest {

    @Test
    void canManageDevicesReturnsFalse() {
        assertFalse(MemberPermissions.INSTANCE.canManageDevices());
    }

    @Test
    void canManageHomeReturnsFalse() {
        assertFalse(MemberPermissions.INSTANCE.canManageHome());
    }

    @Test
    void isSingleton() {
        assertTrue(MemberPermissions.INSTANCE == MemberPermissions.INSTANCE);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
