package com.energymonitor.alert.application.usecase;

import com.energymonitor.alert.api.AlertRaised;
import com.energymonitor.alert.application.command.RegisterThresholdAlertCommand;
import com.energymonitor.alert.application.port.in.RegisterThresholdAlert;
import com.energymonitor.alert.application.port.out.AlertEventPort;
import com.energymonitor.alert.application.port.out.AlertPersistencePort;
import com.energymonitor.alert.application.port.out.ConsumptionLevelLookupPort;
import com.energymonitor.alert.application.port.out.HomeLookupPort;
import com.energymonitor.alert.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.alert.application.result.AlertResult;
import com.energymonitor.alert.domain.model.Alert;
import com.energymonitor.measurement.api.RiskConsumption;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates a recorded measurement and raises a THRESHOLD alert when the reading falls
 * into a HIGH or CRITICAL consumption level.
 *
 * <p>This is the class diagram's {@code AlertObserver} + {@code AlertFactory.createFromMeasurement(m, risk)}
 * pair: the observer arrives as the {@code MeasurementRecordedListener} adapter, the
 * classification as the {@link ConsumptionLevelLookupPort}, and the factory as
 * {@link Alert#threshold}.
 *
 * <p><strong>Alerting rule (initial, documented):</strong> only HIGH and CRITICAL readings
 * raise an alert. The message key follows the level: {@code alert.threshold.high} or
 * {@code alert.threshold.critical}.
 *
 * <p><strong>Home resolution:</strong> the home comes from the device through
 * {@link HomeLookupPort}. While the Devices module does not expose the relationship, the
 * lookup resolves nothing and the reading is skipped — no alert is raised against a
 * guessed home.
 *
 * <p><strong>Transactional boundary:</strong> a single write, so no transaction is
 * needed. This service contains no Spring annotations.
 */
public class RegisterThresholdAlertService implements RegisterThresholdAlert {

    private static final Map<RiskConsumption, String> MESSAGE_KEY_BY_RISK = Map.of(
            RiskConsumption.HIGH, "alert.threshold.high",
            RiskConsumption.CRITICAL, "alert.threshold.critical");

    private final AlertPersistencePort alertPort;
    private final ConsumptionLevelLookupPort levels;
    private final HomeLookupPort homes;
    private final IdentifierGeneratorPort identifiers;
    private final AlertEventPort events;

    public RegisterThresholdAlertService(AlertPersistencePort alertPort,
                                         ConsumptionLevelLookupPort levels,
                                         HomeLookupPort homes,
                                         IdentifierGeneratorPort identifiers,
                                         AlertEventPort events) {
        this.alertPort = alertPort;
        this.levels = levels;
        this.homes = homes;
        this.identifiers = identifiers;
        this.events = events;
    }

    @Override
    public Optional<AlertResult> register(RegisterThresholdAlertCommand command) {
        var level = levels.classify(command.activePower());
        if (level.isEmpty()) {
            return Optional.empty();
        }
        String messageKey = MESSAGE_KEY_BY_RISK.get(level.get().name());
        if (messageKey == null) {
            // LOW and MEDIUM readings never alert
            return Optional.empty();
        }

        var homeId = homes.findHomeIdByDeviceId(command.deviceId());
        if (homeId.isEmpty()) {
            // Documented gap: the Devices module does not expose DeviceHome yet
            return Optional.empty();
        }

        Alert alert = Alert.threshold(identifiers.generate(), homeId.get(), command.deviceId(),
                messageKey, command.dateTime(), level.get().idConsumptionLevel(),
                command.measurementId());
        alertPort.save(alert);

        events.publish(new AlertRaised(alert.idAlert(), alert.homeId(), alert.deviceId(),
                alert.type(), alert.messageKey(), alert.dateTime(),
                alert.consumptionLevelId(), alert.measurementId()));

        return Optional.of(AlertResult.from(alert));
    }
}
