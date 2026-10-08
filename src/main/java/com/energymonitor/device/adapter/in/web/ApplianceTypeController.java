package com.energymonitor.device.adapter.in.web;

import com.energymonitor.device.adapter.in.web.dto.ApplianceTypeResponse;
import com.energymonitor.device.application.port.in.ListApplianceTypes;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The appliance catalog. {@code name} is a stable key (e.g. {@code refrigerator}); clients
 * translate it.
 */
@RestController
@RequestMapping("/api/v1/appliance-types")
public class ApplianceTypeController {

    private final ListApplianceTypes listApplianceTypes;

    public ApplianceTypeController(ListApplianceTypes listApplianceTypes) {
        this.listApplianceTypes = listApplianceTypes;
    }

    @GetMapping
    public List<ApplianceTypeResponse> list() {
        return listApplianceTypes.list().stream()
                .map(e -> new ApplianceTypeResponse(e.idApplianceType(), e.name()))
                .toList();
    }
}
