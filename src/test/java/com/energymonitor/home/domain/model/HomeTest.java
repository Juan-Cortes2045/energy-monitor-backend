package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class HomeTest {

    private static final Instant CREATION_DATE = Instant.parse("2026-01-15T10:30:00Z");

    private Home createHome() {
        return Home.create("hom0000001", "Casa", "hous000001", "Calle 123", "ABC12345", "Mi casa", CREATION_DATE);
    }

    @Test
    void createHomeWithValidData() {
        Home home = createHome();
        assertEquals("hom0000001", home.idHome());
        assertEquals("Casa", home.name());
        assertEquals("hous000001", home.homeTypeId());
        assertEquals("Calle 123", home.address());
        assertEquals("ABC12345", home.accessCode());
        assertEquals("Mi casa", home.description());
        assertEquals(CREATION_DATE, home.creationDate());
    }

    @Test
    void createHomeWithNullDescription() {
        Home home = Home.create("hom0000001", "Casa", "hous000001", "Calle 123", "ABC12345", null, CREATION_DATE);
        assertNull(home.description());
    }

    @Test
    void createHomeWithBlankIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("", "Casa", "hous000001", "Calle 123", "ABC12345", null, CREATION_DATE));
    }

    @Test
    void createHomeWithTooLongIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom00000012345", "Casa", "hous000001", "Calle 123", "ABC12345", null, CREATION_DATE));
    }

    @Test
    void createHomeWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "", "hous000001", "Calle 123", "ABC12345", null, CREATION_DATE));
    }

    @Test
    void createHomeWithTooLongNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "a".repeat(51), "hous000001", "Calle 123", "ABC12345", null, CREATION_DATE));
    }

    @Test
    void createHomeWithBlankAddressThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "Casa", "hous000001", "", "ABC12345", null, CREATION_DATE));
    }

    @Test
    void createHomeWithTooLongAddressThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "Casa", "hous000001", "a".repeat(201), "ABC12345", null, CREATION_DATE));
    }

    @Test
    void createHomeWithBlankAccessCodeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "Casa", "hous000001", "Calle 123", "", null, CREATION_DATE));
    }

    @Test
    void createHomeWithTooLongAccessCodeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "Casa", "hous000001", "Calle 123", "ABC123456", null, CREATION_DATE));
    }

    @Test
    void createHomeWithNullCreationDateThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Home.create("hom0000001", "Casa", "hous000001", "Calle 123", "ABC12345", null, null));
    }

    @Test
    void matchesAccessCodeReturnsTrueForCorrectCode() {
        Home home = createHome();
        assertTrue(home.matchesAccessCode("ABC12345"));
    }

    @Test
    void matchesAccessCodeReturnsFalseForIncorrectCode() {
        Home home = createHome();
        assertFalse(home.matchesAccessCode("WRONG123"));
    }

    @Test
    void equalsBasedOnId() {
        Home home1 = createHome();
        Home home2 = Home.create("hom0000001", "Otro nombre", "apar000001", "Otra direccion", "XYZ98765", null, CREATION_DATE);
        assertEquals(home1, home2);
    }

    @Test
    void notEqualsWhenDifferentId() {
        Home home1 = createHome();
        Home home2 = Home.create("hom0000002", "Casa", "hous000001", "Calle 123", "ABC12345", "Mi casa", CREATION_DATE);
        assertNotEquals(home1, home2);
    }

    @Test
    void notEqualsWhenDifferentType() {
        Home home = createHome();
        assertNotEquals(home, "not a home");
    }
}
