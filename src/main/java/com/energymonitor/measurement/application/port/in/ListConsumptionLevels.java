package com.energymonitor.measurement.application.port.in;

import com.energymonitor.measurement.application.command.ListConsumptionLevelsQuery;
import com.energymonitor.measurement.application.result.ConsumptionLevelResult;
import java.util.List;

/**
 * Input port for listing the consumption level catalog.
 */
public interface ListConsumptionLevels {

    /**
     * @param query no parameters
     * @return the active levels, ordered by {@code minLimit} ascending
     */
    List<ConsumptionLevelResult> list(ListConsumptionLevelsQuery query);
}
