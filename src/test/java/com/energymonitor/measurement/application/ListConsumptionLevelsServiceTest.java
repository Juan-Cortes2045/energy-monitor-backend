package com.energymonitor.measurement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.command.ListConsumptionLevelsQuery;
import com.energymonitor.measurement.application.usecase.ListConsumptionLevelsService;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListConsumptionLevelsServiceTest {

    private MeasurementUseCaseFixtures.FakeConsumptionLevelPersistencePort levelPort;
    private ListConsumptionLevelsService service;

    @BeforeEach
    void setUp() {
        levelPort = new MeasurementUseCaseFixtures.FakeConsumptionLevelPersistencePort();
        service = new ListConsumptionLevelsService(levelPort);
    }

    @Test
    void listReturnsLevelsOrderedByMinLimit() {
        // Seeded out of order on purpose
        levelPort.seed(ConsumptionLevel.create("high000001", RiskConsumption.HIGH, "high", 1500, 3000));
        levelPort.seed(ConsumptionLevel.create("low0000001", RiskConsumption.LOW, "low", 0, 500));
        levelPort.seed(ConsumptionLevel.create("crit000001", RiskConsumption.CRITICAL, "critical", 3000, 1000000));
        levelPort.seed(ConsumptionLevel.create("medi000001", RiskConsumption.MEDIUM, "medium", 500, 1500));

        var results = service.list(new ListConsumptionLevelsQuery());

        assertEquals(4, results.size());
        assertEquals(RiskConsumption.LOW, results.get(0).name());
        assertEquals(RiskConsumption.MEDIUM, results.get(1).name());
        assertEquals(RiskConsumption.HIGH, results.get(2).name());
        assertEquals(RiskConsumption.CRITICAL, results.get(3).name());
    }

    @Test
    void listWithEmptyCatalogReturnsEmpty() {
        assertTrue(service.list(new ListConsumptionLevelsQuery()).isEmpty());
    }
}
