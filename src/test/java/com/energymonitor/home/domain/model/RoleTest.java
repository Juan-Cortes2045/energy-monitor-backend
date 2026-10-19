package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void hasExactlyTwoValues() {
        assertEquals(2, Role.values().length);
    }

    @Test
    void ownerValueExists() {
        assertEquals(Role.OWNER, Role.valueOf("OWNER"));
    }

    @Test
    void memberValueExists() {
        assertEquals(Role.MEMBER, Role.valueOf("MEMBER"));
    }
}
