package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.home.application.command.CreateHomeCommand;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeTypeNotFoundException;
import com.energymonitor.home.application.usecase.CreateHomeService;
import com.energymonitor.home.domain.model.HomeType;
import com.energymonitor.home.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateHomeServiceTest {

    private HomeUseCaseFixtures.FakeHomePersistencePort homePort;
    private HomeUseCaseFixtures.FakeHomeTypePersistencePort homeTypePort;
    private HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort thresholdsPort;
    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private HomeUseCaseFixtures.FakeIdentifierGeneratorPort identifiers;
    private HomeUseCaseFixtures.FakeAccessCodeGeneratorPort accessCodes;
    private HomeUseCaseFixtures.FakeSystemDefaultsPort defaults;
    private CreateHomeService service;

    @BeforeEach
    void setUp() {
        homePort = new HomeUseCaseFixtures.FakeHomePersistencePort();
        homeTypePort = new HomeUseCaseFixtures.FakeHomeTypePersistencePort();
        thresholdsPort = new HomeUseCaseFixtures.FakeHomeThresholdsPersistencePort();
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();
        identifiers = new HomeUseCaseFixtures.FakeIdentifierGeneratorPort();
        accessCodes = new HomeUseCaseFixtures.FakeAccessCodeGeneratorPort();
        defaults = new HomeUseCaseFixtures.FakeSystemDefaultsPort();

        homeTypePort.seed(new HomeType("hous000001", "house"));

        service = new CreateHomeService(homePort, homeTypePort, thresholdsPort, userHomePort,
                identifiers, accessCodes, defaults, HomeUseCaseFixtures.clock());
    }

    @Test
    void createHomeWithValidData() {
        var command = new CreateHomeCommand("use0000001", "Casa", "hous000001", "Calle 123", "Mi casa");
        var result = service.create(command);

        assertNotNull(result);
        assertEquals("Casa", result.name());
        assertEquals("hous000001", result.homeTypeId());
        assertEquals("Calle 123", result.address());
        assertEquals("Mi casa", result.description());
        assertNotNull(result.accessCode());
        assertEquals(8, result.accessCode().length());
        assertNotNull(result.creationDate());

        // Verify home was saved
        assertTrue(homePort.findActive(result.idHome()).isPresent());

        // Verify thresholds were created with system defaults
        var thresholds = thresholdsPort.findActiveByHomeId(result.idHome()).orElseThrow();
        assertEquals(HomeUseCaseFixtures.DEFAULT_DAILY_LIMIT, thresholds.dailyLimit());
        assertEquals(HomeUseCaseFixtures.DEFAULT_MONTHLY_LIMIT, thresholds.monthlyLimit());
        assertTrue(thresholds.isUseSystemDefault());

        // Verify OWNER membership was created
        var membership = userHomePort.findActive("use0000001", result.idHome()).orElseThrow();
        assertEquals(Role.OWNER, membership.role());
    }

    @Test
    void createHomeWithNullDescription() {
        var command = new CreateHomeCommand("use0000001", "Casa", "hous000001", "Calle 123", null);
        var result = service.create(command);
        assertNotNull(result);
    }

    @Test
    void createHomeWithNonExistentHomeTypeThrows() {
        var command = new CreateHomeCommand("use0000001", "Casa", "apar000001", "Calle 123", null);
        assertThrows(HomeTypeNotFoundException.class, () -> service.create(command));
    }

    @Test
    void createHomePropagatesFailureIfHomeSaveFails() {
        // Create a port that throws on save
        HomeUseCaseFixtures.FakeHomePersistencePort failingPort = new HomeUseCaseFixtures.FakeHomePersistencePort() {
            @Override
            public com.energymonitor.home.domain.model.Home save(com.energymonitor.home.domain.model.Home home) {
                throw new RuntimeException("database error");
            }
        };

        CreateHomeService failingService = new CreateHomeService(failingPort, homeTypePort, thresholdsPort,
                userHomePort, identifiers, accessCodes, defaults, HomeUseCaseFixtures.clock());

        var command = new CreateHomeCommand("use0000001", "Casa", "hous000001", "Calle 123", null);
        assertThrows(RuntimeException.class, () -> failingService.create(command));

        // Document: in Etapa E, the transactional decorator ensures atomicity
    }

    @Test
    void createHomeWithAccessCodeCollisionRetries() {
        // Pre-occupy the first generated code
        homePort.seed(com.energymonitor.home.domain.model.Home.create(
                "hom0000001", "Existing", "hous000001", "Calle 1", "ac000001", null,
                HomeUseCaseFixtures.NOW));

        var command = new CreateHomeCommand("use0000001", "Casa", "hous000001", "Calle 123", null);
        var result = service.create(command);

        assertNotNull(result);
        assertTrue(!result.accessCode().equals("ac000001"));
    }

    @Test
    void createHomeThrowsWhenAccessCodeCollisionExhausted() {
        // Pre-occupy all possible codes
        for (int i = 1; i <= 5; i++) {
            homePort.seed(com.energymonitor.home.domain.model.Home.create(
                    "hom000000" + i, "Existing" + i, "hous000001", "Calle " + i,
                    String.format("ac%06d", i), null, HomeUseCaseFixtures.NOW));
        }

        var command = new CreateHomeCommand("use0000001", "Casa", "hous000001", "Calle 123", null);
        assertThrows(HomeConflictException.class, () -> service.create(command));
    }
}
