package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.application.command.ToggleFavoriteCommand;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.usecase.ToggleFavoriteService;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToggleFavoriteServiceTest {

    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private ToggleFavoriteService service;

    @BeforeEach
    void setUp() {
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();
        userHomePort.save(UserHome.member("use0000001", "hom0000001"));
        service = new ToggleFavoriteService(userHomePort);
    }

    @Test
    void toggleFavoriteSwitchesFlag() {
        var command = new ToggleFavoriteCommand("use0000001", "hom0000001");

        // First toggle: false -> true
        var result1 = service.toggle(command);
        assertTrue(result1.favorite());

        // Second toggle: true -> false
        var result2 = service.toggle(command);
        assertFalse(result2.favorite());
    }

    @Test
    void toggleFavoriteWithoutMembershipThrows() {
        var command = new ToggleFavoriteCommand("use0000002", "hom0000001");
        assertThrows(UserHomeNotFoundException.class, () -> service.toggle(command));
    }
}
