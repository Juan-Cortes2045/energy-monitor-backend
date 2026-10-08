package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.ListMembersQuery;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.port.in.ListMembers;
import com.energymonitor.home.application.port.out.UserDirectoryPort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.application.result.UserHomeResult;
import java.util.List;

/**
 * Lists the active members of a home.
 *
 * <p>The actor must be an active member of the home. If not, {@link HomeNotFoundException}
 * is thrown (404) to avoid leaking the existence of homes the user does not belong to.
 */
public class ListMembersService implements ListMembers {

    private final UserHomePersistencePort userHomePort;
    private final UserDirectoryPort userDirectory;

    public ListMembersService(UserHomePersistencePort userHomePort,
                              UserDirectoryPort userDirectory) {
        this.userHomePort = userHomePort;
        this.userDirectory = userDirectory;
    }

    @Override
    public List<UserHomeResult> list(ListMembersQuery query) {
        // Verify membership (404 if not a member, to avoid leaking existence)
        userHomePort.findActive(query.userId(), query.homeId())
                .orElseThrow(() -> new HomeNotFoundException(
                        "no active membership for user " + query.userId() + " in home " + query.homeId()));

        var members = userHomePort.listActiveByHomeId(query.homeId());
        var people = userDirectory.findByIds(members.stream().map(m -> m.userId()).toList());
        return members.stream()
                .map(m -> UserHomeResult.from(m, people.get(m.userId())))
                .toList();
    }
}
