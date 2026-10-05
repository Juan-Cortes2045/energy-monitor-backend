package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.ListMembersQuery;
import com.energymonitor.home.application.result.UserHomeResult;
import java.util.List;

/**
 * Input port for listing the members of a home.
 */
public interface ListMembers {

    /**
     * @param query the actor and the home whose members are listed
     * @return the active memberships of the home
     */
    List<UserHomeResult> list(ListMembersQuery query);
}
