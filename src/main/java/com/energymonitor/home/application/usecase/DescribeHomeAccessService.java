package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.port.in.DescribeHomeAccess;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import java.util.Optional;

/**
 * Answers the device service, in one call, whether a home exists and what the caller is in it.
 * A deleted home grants nothing, whatever memberships were left behind.
 */
public class DescribeHomeAccessService implements DescribeHomeAccess {

    private final HomePersistencePort homePort;
    private final UserHomePersistencePort userHomePort;

    public DescribeHomeAccessService(HomePersistencePort homePort, UserHomePersistencePort userHomePort) {
        this.homePort = homePort;
        this.userHomePort = userHomePort;
    }

    @Override
    public HomeAccess describe(String userId, String homeId) {
        if (homeId == null || homePort.findActive(homeId).isEmpty()) {
            return new HomeAccess(false, false, false);
        }
        Optional<UserHome> membership = userId == null
                ? Optional.empty() : userHomePort.findActive(userId, homeId);
        return new HomeAccess(true, membership.isPresent(),
                membership.map(m -> m.role() == Role.OWNER).orElse(false));
    }
}
