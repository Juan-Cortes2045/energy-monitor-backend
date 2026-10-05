package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.ListHomesQuery;
import com.energymonitor.home.application.port.in.ListHomes;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.application.result.HomeMembershipResult;
import java.util.List;

/**
 * Lists all homes where the user is a member, with role and favorite information.
 *
 * <p>Uses a batch query ({@code findActiveByIds}) to avoid N+1.
 */
public class ListHomesService implements ListHomes {

    private final HomePersistencePort homePort;
    private final UserHomePersistencePort userHomePort;

    public ListHomesService(HomePersistencePort homePort,
                            UserHomePersistencePort userHomePort) {
        this.homePort = homePort;
        this.userHomePort = userHomePort;
    }

    @Override
    public List<HomeMembershipResult> list(ListHomesQuery query) {
        // Get all memberships of the user
        var memberships = userHomePort.listActiveByUser(query.userId());

        // Batch load all homes (avoids N+1)
        var homeIds = memberships.stream()
                .map(m -> m.homeId())
                .toList();
        var homes = homePort.findActiveByIds(homeIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        h -> h.idHome(), h -> h));

        // Combine home data with membership info
        return memberships.stream()
                .map(m -> {
                    var home = homes.get(m.homeId());
                    return HomeMembershipResult.from(home, m);
                })
                .toList();
    }
}
