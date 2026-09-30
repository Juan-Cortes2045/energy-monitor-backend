package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.ListHomesQuery;
import com.energymonitor.home.application.result.HomeMembershipResult;
import java.util.List;

/**
 * Input port for listing the homes of a user.
 */
public interface ListHomes {

    /**
     * @param query the user whose homes are listed
     * @return the homes with membership information
     */
    List<HomeMembershipResult> list(ListHomesQuery query);
}
