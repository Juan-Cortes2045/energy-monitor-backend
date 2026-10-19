package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.application.command.ListConsumptionLevelsQuery;
import com.energymonitor.measurement.application.port.in.ListConsumptionLevels;
import com.energymonitor.measurement.application.port.out.ConsumptionLevelPersistencePort;
import com.energymonitor.measurement.application.result.ConsumptionLevelResult;
import com.energymonitor.measurement.domain.model.ConsumptionLevel;
import java.util.Comparator;
import java.util.List;

/**
 * Lists the consumption level catalog, ordered by {@code minLimit} ascending so the
 * response is stable regardless of how the store returns the rows.
 *
 * <p>Read-only: no transaction boundary. This service contains no Spring annotations.
 */
public class ListConsumptionLevelsService implements ListConsumptionLevels {

    private final ConsumptionLevelPersistencePort levelPort;

    public ListConsumptionLevelsService(ConsumptionLevelPersistencePort levelPort) {
        this.levelPort = levelPort;
    }

    @Override
    public List<ConsumptionLevelResult> list(ListConsumptionLevelsQuery query) {
        return levelPort.listActive().stream()
                .sorted(Comparator.comparingDouble(ConsumptionLevel::minLimit))
                .map(ConsumptionLevelResult::from)
                .toList();
    }
}
