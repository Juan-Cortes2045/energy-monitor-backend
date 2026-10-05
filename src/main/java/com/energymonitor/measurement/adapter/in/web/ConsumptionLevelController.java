package com.energymonitor.measurement.adapter.in.web;

import com.energymonitor.measurement.adapter.in.web.dto.ConsumptionLevelResponse;
import com.energymonitor.measurement.application.command.ListConsumptionLevelsQuery;
import com.energymonitor.measurement.application.port.in.ListConsumptionLevels;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the consumption level catalog (read-only).
 */
@RestController
@RequestMapping("/api/v1/consumption-levels")
public class ConsumptionLevelController {

    private final ListConsumptionLevels listConsumptionLevels;

    public ConsumptionLevelController(ListConsumptionLevels listConsumptionLevels) {
        this.listConsumptionLevels = listConsumptionLevels;
    }

    @GetMapping
    public List<ConsumptionLevelResponse> listConsumptionLevels() {
        return listConsumptionLevels.list(new ListConsumptionLevelsQuery()).stream()
                .map(result -> new ConsumptionLevelResponse(result.idConsumptionLevel(),
                        result.name(), result.description(), result.minLimit(), result.maxLimit()))
                .toList();
    }
}
