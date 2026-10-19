package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomeThresholdsTest {

    private HomeThresholds createThresholds() {
        return HomeThresholds.create("thr0000001", "hom0000001", 10.0, 300.0, true);
    }

    @Test
    void createThresholdsWithValidData() {
        HomeThresholds thresholds = createThresholds();
        assertEquals("thr0000001", thresholds.idThreshold());
        assertEquals("hom0000001", thresholds.homeId());
        assertEquals(10.0, thresholds.dailyLimit());
        assertEquals(300.0, thresholds.monthlyLimit());
        assertTrue(thresholds.isUseSystemDefault());
    }

    @Test
    void createWithZeroDailyLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> HomeThresholds.create("thr0000001", "hom0000001", 0.0, 300.0, true));
    }

    @Test
    void createWithNegativeDailyLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> HomeThresholds.create("thr0000001", "hom0000001", -5.0, 300.0, true));
    }

    @Test
    void createWithZeroMonthlyLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> HomeThresholds.create("thr0000001", "hom0000001", 10.0, 0.0, true));
    }

    @Test
    void createWithNegativeMonthlyLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> HomeThresholds.create("thr0000001", "hom0000001", 10.0, -100.0, true));
    }

    @Test
    void createWithDailyGreaterThanMonthlyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> HomeThresholds.create("thr0000001", "hom0000001", 500.0, 300.0, true));
    }

    @Test
    void createWithDailyEqualToMonthlyIsValid() {
        HomeThresholds thresholds = HomeThresholds.create("thr0000001", "hom0000001", 300.0, 300.0, true);
        assertEquals(300.0, thresholds.dailyLimit());
        assertEquals(300.0, thresholds.monthlyLimit());
    }

    @Test
    void updateWithValidValues() {
        HomeThresholds thresholds = createThresholds();
        thresholds.update(15.0, 450.0);
        assertEquals(15.0, thresholds.dailyLimit());
        assertEquals(450.0, thresholds.monthlyLimit());
        assertFalse(thresholds.isUseSystemDefault());
    }

    @Test
    void updateWithZeroDailyThrows() {
        HomeThresholds thresholds = createThresholds();
        assertThrows(IllegalArgumentException.class, () -> thresholds.update(0.0, 450.0));
    }

    @Test
    void updateWithNegativeMonthlyThrows() {
        HomeThresholds thresholds = createThresholds();
        assertThrows(IllegalArgumentException.class, () -> thresholds.update(15.0, -100.0));
    }

    @Test
    void updateWithDailyGreaterThanMonthlyThrows() {
        HomeThresholds thresholds = createThresholds();
        assertThrows(IllegalArgumentException.class, () -> thresholds.update(500.0, 300.0));
    }

    @Test
    void updateWithDailyEqualToMonthlyIsValid() {
        HomeThresholds thresholds = createThresholds();
        thresholds.update(300.0, 300.0);
        assertEquals(300.0, thresholds.dailyLimit());
        assertEquals(300.0, thresholds.monthlyLimit());
    }

    @Test
    void resetToDefaultsSetsUseSystemDefaultTrue() {
        HomeThresholds thresholds = createThresholds();
        thresholds.update(15.0, 450.0);
        assertFalse(thresholds.isUseSystemDefault());
        thresholds.resetToDefaults(10.0, 300.0);
        assertEquals(10.0, thresholds.dailyLimit());
        assertEquals(300.0, thresholds.monthlyLimit());
        assertTrue(thresholds.isUseSystemDefault());
    }

    @Test
    void resetToDefaultsWithZeroDailyThrows() {
        HomeThresholds thresholds = createThresholds();
        assertThrows(IllegalArgumentException.class, () -> thresholds.resetToDefaults(0.0, 300.0));
    }

    @Test
    void resetToDefaultsWithDailyGreaterThanMonthlyThrows() {
        HomeThresholds thresholds = createThresholds();
        assertThrows(IllegalArgumentException.class, () -> thresholds.resetToDefaults(500.0, 300.0));
    }

    @Test
    void equalsBasedOnIdThreshold() {
        HomeThresholds t1 = createThresholds();
        HomeThresholds t2 = HomeThresholds.create("thr0000001", "hom0000002", 20.0, 600.0, false);
        assertEquals(t1, t2);
    }

    @Test
    void notEqualsWhenDifferentIdThreshold() {
        HomeThresholds t1 = createThresholds();
        HomeThresholds t2 = HomeThresholds.create("thr0000002", "hom0000001", 10.0, 300.0, true);
        assertNotEquals(t1, t2);
    }
}
