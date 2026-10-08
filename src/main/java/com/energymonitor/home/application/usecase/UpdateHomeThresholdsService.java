package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.UpdateHomeThresholdsCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.port.in.UpdateHomeThresholds;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.application.result.HomeThresholdsResult;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;

/**
 * Updates the consumption thresholds of a home.
 *
 * <p>The user must be an active member with {@code canManageHome()} permission.
 * Validation errors from the domain ({@link IllegalArgumentException}) are not caught
 * here; they are mapped to HTTP 400 in the web layer.
 */
public class UpdateHomeThresholdsService implements UpdateHomeThresholds {

    private final HomeThresholdsPersistencePort thresholdsPort;
    private final UserHomePersistencePort userHomePort;

    public UpdateHomeThresholdsService(HomeThresholdsPersistencePort thresholdsPort,
                                       UserHomePersistencePort userHomePort) {
        this.thresholdsPort = thresholdsPort;
        this.userHomePort = userHomePort;
    }

    @Override
    public HomeThresholdsResult update(UpdateHomeThresholdsCommand command) {
        // Verify membership (404 if not a member)
        UserHome membership = userHomePort.findActive(command.userId(), command.homeId())
                .orElseThrow(() -> new HomeNotFoundException(
                        "no active membership for user " + command.userId() + " in home " + command.homeId()));

        // Verify permission (403 if member without canManageHome)
        if (!membership.getPermissions().canManageHome()) {
            throw new HomeAccessDeniedException(
                    "user " + command.userId() + " cannot manage home " + command.homeId());
        }

        // Load thresholds
        HomeThresholds thresholds = thresholdsPort.findActiveByHomeId(command.homeId())
                .orElseThrow(() -> new HomeNotFoundException(
                        "no thresholds found for home " + command.homeId()));

        // Delegate validation to domain (IllegalArgumentException propagates to web layer)
        thresholds.setLimit(command.limitPeriod(), command.limit());
        thresholdsPort.save(thresholds);

        return HomeThresholdsResult.from(thresholds);
    }
}
