package com.energymonitor.measurement.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.api.RiskConsumption;
import org.junit.jupiter.api.Test;

class ConsumptionLevelTest {

    private ConsumptionLevel createLevel() {
        return ConsumptionLevel.create("low0000001", RiskConsumption.LOW,
                "Efficient consumption", 0, 500);
    }

    @Test
    void createLevelWithValidData() {
        ConsumptionLevel level = createLevel();
        assertEquals("low0000001", level.idConsumptionLevel());
        assertEquals(RiskConsumption.LOW, level.name());
        assertEquals("Efficient consumption", level.description());
        assertEquals(0, level.minLimit());
        assertEquals(500, level.maxLimit());
    }

    @Test
    void rangeIncludesMinLimit() {
        assertTrue(createLevel().contains(0));
    }

    @Test
    void rangeExcludesMaxLimit() {
        // [min, max): the boundary belongs to the next level (MEAS-INV-010)
        assertFalse(createLevel().contains(500));
    }

    @Test
    void rangeContainsInnerValues() {
        assertTrue(createLevel().contains(250.5));
    }

    @Test
    void rangeRejectsValuesBelowMin() {
        assertFalse(createLevel().contains(-0.1));
    }

    @Test
    void createLevelWithMaxEqualToMinThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low0000001", RiskConsumption.LOW, "desc", 500, 500));
    }

    @Test
    void createLevelWithMaxBelowMinThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low0000001", RiskConsumption.LOW, "desc", 500, 100));
    }

    @Test
    void createLevelWithNegativeMinLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low0000001", RiskConsumption.LOW, "desc", -1, 500));
    }

    @Test
    void createLevelWithNullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low0000001", null, "desc", 0, 500));
    }

    @Test
    void createLevelWithBlankIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("", RiskConsumption.LOW, "desc", 0, 500));
    }

    @Test
    void createLevelWithTooLongIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low00000012345", RiskConsumption.LOW, "desc", 0, 500));
    }

    @Test
    void createLevelWithBlankDescriptionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low0000001", RiskConsumption.LOW, "", 0, 500));
    }

    @Test
    void createLevelWithTooLongDescriptionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumptionLevel.create("low0000001", RiskConsumption.LOW, "a".repeat(201), 0, 500));
    }

    @Test
    void equalityIsBasedOnId() {
        ConsumptionLevel first = createLevel();
        ConsumptionLevel second = ConsumptionLevel.create("low0000001", RiskConsumption.HIGH, "other", 10, 20);
        ConsumptionLevel different = ConsumptionLevel.create("medi000001", RiskConsumption.LOW, "desc", 0, 500);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }
}
