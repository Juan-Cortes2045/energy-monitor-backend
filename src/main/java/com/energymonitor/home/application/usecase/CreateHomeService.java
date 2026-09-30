package com.energymonitor.home.application.usecase;

import com.energymonitor.home.application.command.CreateHomeCommand;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeTypeNotFoundException;
import com.energymonitor.home.application.port.in.CreateHome;
import com.energymonitor.home.application.port.out.AccessCodeGeneratorPort;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.HomeTypePersistencePort;
import com.energymonitor.home.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.home.application.port.out.SystemDefaultsPort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.application.result.HomeResult;
import com.energymonitor.home.domain.model.Home;
import com.energymonitor.home.domain.model.HomeThresholds;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Clock;
import java.time.Instant;

/**
 * Creates a new home with the user as OWNER.
 *
 * <p><strong>Transactional boundary:</strong> This use case performs three writes that
 * must execute atomically:
 * <ol>
 *   <li>{@code home} (the aggregate root)</li>
 *   <li>{@code home_thresholds} (with {@code useSystemDefault = true})</li>
 *   <li>{@code user_home} (OWNER membership)</li>
 * </ol>
 * If any write fails, the entire operation must roll back. The actual transaction
 * management is configured in {@code infrastructure/} (Etapa E) via a transactional
 * decorator. This service contains no Spring annotations.
 */
public class CreateHomeService implements CreateHome {

    private static final int MAX_ACCESS_CODE_ATTEMPTS = 5;

    private final HomePersistencePort homePort;
    private final HomeTypePersistencePort homeTypePort;
    private final HomeThresholdsPersistencePort thresholdsPort;
    private final UserHomePersistencePort userHomePort;
    private final IdentifierGeneratorPort identifiers;
    private final AccessCodeGeneratorPort accessCodes;
    private final SystemDefaultsPort defaults;
    private final Clock clock;

    public CreateHomeService(HomePersistencePort homePort,
                             HomeTypePersistencePort homeTypePort,
                             HomeThresholdsPersistencePort thresholdsPort,
                             UserHomePersistencePort userHomePort,
                             IdentifierGeneratorPort identifiers,
                             AccessCodeGeneratorPort accessCodes,
                             SystemDefaultsPort defaults,
                             Clock clock) {
        this.homePort = homePort;
        this.homeTypePort = homeTypePort;
        this.thresholdsPort = thresholdsPort;
        this.userHomePort = userHomePort;
        this.identifiers = identifiers;
        this.accessCodes = accessCodes;
        this.defaults = defaults;
        this.clock = clock;
    }

    @Override
    public HomeResult create(CreateHomeCommand command) {
        // Validate home type exists
        homeTypePort.findActive(command.homeTypeId())
                .orElseThrow(() -> new HomeTypeNotFoundException(
                        "no active home type " + command.homeTypeId()));

        // Generate unique access code with bounded retry
        String accessCode = generateUniqueAccessCode();

        // Create home
        Instant now = clock.instant();
        String idHome = identifiers.generate();
        Home home = Home.create(idHome, command.name(), command.homeTypeId(),
                command.address(), accessCode, command.description(), now);
        homePort.save(home);

        // Create thresholds with system defaults
        HomeThresholds thresholds = HomeThresholds.create(
                identifiers.generate(), idHome,
                defaults.defaultDailyLimit(), defaults.defaultMonthlyLimit(), true);
        thresholdsPort.save(thresholds);

        // Create OWNER membership
        UserHome membership = UserHome.owner(command.userId(), idHome);
        userHomePort.save(membership);

        return HomeResult.from(home);
    }

    private String generateUniqueAccessCode() {
        for (int attempt = 0; attempt < MAX_ACCESS_CODE_ATTEMPTS; attempt++) {
            String code = accessCodes.generate();
            if (!homePort.existsActiveByAccessCode(code)) {
                return code;
            }
        }
        throw new HomeConflictException(
                "could not generate a unique access code after " + MAX_ACCESS_CODE_ATTEMPTS + " attempts");
    }
}
