package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.port.out.SystemDefaultsPort;
import com.energymonitor.home.application.command.GetHomeThresholdsQuery;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.port.in.GetHomeThresholds;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.application.result.HomeThresholdsResult;

/**
 * Retrieves the thresholds of a home.
 *
 * <p>The user must be an active member of the home. If not, {@link HomeNotFoundException}
 * is thrown (404) to avoid leaking the existence of homes the user does not belong to.
 */
public class GetHomeThresholdsService implements GetHomeThresholds {

    private final HomeThresholdsPersistencePort thresholdsPort;
    private final UserHomePersistencePort userHomePort;
    private final SystemDefaultsPort defaults;

    public GetHomeThresholdsService(HomeThresholdsPersistencePort thresholdsPort,
                                    UserHomePersistencePort userHomePort, SystemDefaultsPort defaults) {
        this.thresholdsPort = thresholdsPort;
        this.userHomePort = userHomePort;
        this.defaults = defaults;
    }

    @Override
    public HomeThresholdsResult get(GetHomeThresholdsQuery query) {
        // Verify membership (404 if not a member, to avoid leaking existence)
        userHomePort.findActive(query.userId(), query.homeId())
                .orElseThrow(() -> new HomeNotFoundException(
                        "no active membership for user " + query.userId() + " in home " + query.homeId()));

        return thresholdsPort.findActiveByHomeId(query.homeId())
                .map(t -> HomeThresholdsResult.from(t, defaults))
                .orElseThrow(() -> new HomeNotFoundException(
                        "no thresholds found for home " + query.homeId()));
    }
}
