package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.ListHomeTypesQuery;
import com.energymonitor.home.application.port.in.ListHomeTypes;
import com.energymonitor.home.application.port.out.HomeTypePersistencePort;
import com.energymonitor.home.application.result.HomeTypeResult;
import java.util.List;

/**
 * Lists all available home types (read-only catalog).
 */
public class ListHomeTypesService implements ListHomeTypes {

    private final HomeTypePersistencePort homeTypePort;

    public ListHomeTypesService(HomeTypePersistencePort homeTypePort) {
        this.homeTypePort = homeTypePort;
    }

    @Override
    public List<HomeTypeResult> list(ListHomeTypesQuery query) {
        return homeTypePort.findAllActive().stream()
                .map(HomeTypeResult::from)
                .toList();
    }
}
