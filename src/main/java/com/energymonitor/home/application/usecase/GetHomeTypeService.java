package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.GetHomeTypeQuery;
import com.energymonitor.home.application.exception.HomeTypeNotFoundException;
import com.energymonitor.home.application.port.in.GetHomeType;
import com.energymonitor.home.application.port.out.HomeTypePersistencePort;
import com.energymonitor.home.application.result.HomeTypeResult;

/**
 * Retrieves a home type by identifier.
 */
public class GetHomeTypeService implements GetHomeType {

    private final HomeTypePersistencePort homeTypePort;

    public GetHomeTypeService(HomeTypePersistencePort homeTypePort) {
        this.homeTypePort = homeTypePort;
    }

    @Override
    public HomeTypeResult get(GetHomeTypeQuery query) {
        return homeTypePort.findActive(query.idHomeType())
                .map(HomeTypeResult::from)
                .orElseThrow(() -> new HomeTypeNotFoundException(
                        "no active home type " + query.idHomeType()));
    }
}
