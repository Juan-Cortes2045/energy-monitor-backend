package com.energymonitor.home.application.usecase;

import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.application.port.out.HomeEventPort;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import java.time.Clock;

/**
 * The steps that remove a home, shared by the two ways a home ends: an owner deletes it, or the
 * account of its only owner is deleted. Both must leave the same state behind (no memberships, no
 * thresholds, devices unlinked), so they are written once.
 */
final class HomeRemoval {

    private HomeRemoval() {
    }

    static void remove(String homeId, String deletedBy, HomePersistencePort homePort,
                       UserHomePersistencePort userHomePort, HomeThresholdsPersistencePort thresholdsPort,
                       HomeEventPort events, Clock clock) {
        userHomePort.listActiveByHomeId(homeId)
                .forEach(member -> userHomePort.remove(member.userId(), homeId));
        thresholdsPort.removeByHomeId(homeId);
        homePort.remove(homeId);
        events.publish(new HomeDeleted(homeId, deletedBy, clock.instant()));
    }
}
