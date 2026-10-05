package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HomeTypeTest {

    @Test
    void createWithValidData() {
        HomeType type = new HomeType("hous000001", "house");
        assertEquals("hous000001", type.idHomeType());
        assertEquals("house", type.name());
    }

    @Test
    void createWithBlankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new HomeType("", "house"));
    }

    @Test
    void createWithTooLongIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new HomeType("hous0000012345", "house"));
    }

    @Test
    void createWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new HomeType("hous000001", ""));
    }

    @Test
    void createWithTooLongNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new HomeType("hous000001", "a".repeat(51)));
    }

    @Test
    void equalsBasedOnId() {
        HomeType t1 = new HomeType("hous000001", "house");
        HomeType t2 = new HomeType("hous000001", "apartment");
        assertEquals(t1, t2);
    }

    @Test
    void notEqualsWhenDifferentId() {
        HomeType t1 = new HomeType("hous000001", "house");
        HomeType t2 = new HomeType("apar000001", "house");
        assertNotEquals(t1, t2);
    }

    @Test
    void notEqualsWhenDifferentType() {
        HomeType type = new HomeType("hous000001", "house");
        assertNotEquals(type, "not a home type");
    }
}
