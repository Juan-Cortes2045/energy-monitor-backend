package com.energymonitor.home.infrastructure;

import com.energymonitor.home.api.HomeApi;
import com.energymonitor.home.api.HomeDto;
import com.energymonitor.home.api.HomeThresholdsDto;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import com.energymonitor.home.domain.model.HomeThresholds;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link HomeApi} for consumption by other modules.
 */
@Component
public class HomeApiImpl implements HomeApi {

    private final HomePersistencePort homePort;
    private final HomeThresholdsPersistencePort thresholdsPort;
    private final UserHomePersistencePort userHomePort;

    public HomeApiImpl(HomePersistencePort homePort,
                       HomeThresholdsPersistencePort thresholdsPort,
                       UserHomePersistencePort userHomePort) {
        this.homePort = homePort;
        this.thresholdsPort = thresholdsPort;
        this.userHomePort = userHomePort;
    }

    @Override
    public boolean homeExists(String idHome) {
        return homePort.findActive(idHome).isPresent();
    }

    @Override
    public boolean isMember(String idUser, String idHome) {
        return userHomePort.findActive(idUser, idHome).isPresent();
    }

    @Override
    public boolean isOwner(String idUser, String idHome) {
        return userHomePort.findActive(idUser, idHome)
                .filter(membership -> membership.role() == Role.OWNER)
                .isPresent();
    }

    @Override
    public List<String> memberIds(String idHome) {
        return userHomePort.listActiveByHomeId(idHome).stream().map(UserHome::userId).toList();
    }

    @Override
    public Optional<HomeDto> findHome(String idHome) {
        return homePort.findActive(idHome).map(HomeApiImpl::toDto);
    }

    @Override
    public Optional<HomeThresholdsDto> findThresholds(String idHome) {
        return thresholdsPort.findActiveByHomeId(idHome).map(HomeApiImpl::toDto);
    }

    private static HomeDto toDto(Home home) {
        return new HomeDto(home.idHome(), home.name(), home.homeTypeId(),
                home.address(), home.creationDate());
    }

    private static HomeThresholdsDto toDto(HomeThresholds thresholds) {
        return new HomeThresholdsDto(thresholds.idThreshold(), thresholds.homeId(),
                thresholds.dailyLimit(), thresholds.monthlyLimit(), thresholds.isUseSystemDefault(),
                thresholds.limitPeriod());
    }
}
