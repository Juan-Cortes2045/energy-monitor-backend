package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.GetHomeTypeQuery;
import com.energymonitor.home.application.result.HomeTypeResult;

/**
 * Input port for retrieving a home type by identifier.
 */
public interface GetHomeType {

    /**
     * @param query the query data
     * @return the home type
     */
    HomeTypeResult get(GetHomeTypeQuery query);
}
