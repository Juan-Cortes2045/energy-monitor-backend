package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.ToggleFavoriteCommand;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.port.in.ToggleFavorite;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.application.result.UserHomeResult;
import com.energymonitor.home.domain.model.UserHome;

/**
 * Toggles the favorite flag of a home for a user.
 */
public class ToggleFavoriteService implements ToggleFavorite {

    private final UserHomePersistencePort userHomePort;

    public ToggleFavoriteService(UserHomePersistencePort userHomePort) {
        this.userHomePort = userHomePort;
    }

    @Override
    public UserHomeResult toggle(ToggleFavoriteCommand command) {
        UserHome membership = userHomePort.findActive(command.userId(), command.homeId())
                .orElseThrow(() -> new UserHomeNotFoundException(
                        "no active membership for user " + command.userId() + " in home " + command.homeId()));

        membership.toggleFavorite();
        userHomePort.save(membership);

        return UserHomeResult.from(membership);
    }
}
