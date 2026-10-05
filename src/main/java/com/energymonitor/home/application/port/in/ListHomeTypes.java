package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.ListHomeTypesQuery;
import com.energymonitor.home.application.result.HomeTypeResult;
import java.util.List;

/**
 * Input port for listing all home types.
 */
public interface ListHomeTypes {

    /**
     * @param query the query (empty)
     * @return all home types
     */
    List<HomeTypeResult> list(ListHomeTypesQuery query);
}
