package com.energymonitor.measurement.application.usecase;

import com.energymonitor.measurement.api.MeasurementRecorded;
import com.energymonitor.measurement.application.command.RegisterMeasurementCommand;
import com.energymonitor.measurement.application.port.in.RegisterMeasurement;
import com.energymonitor.measurement.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.measurement.application.port.out.MeasurementEventPort;
import com.energymonitor.measurement.application.port.out.MeasurementPersistencePort;
import com.energymonitor.measurement.application.result.MeasurementResult;
import com.energymonitor.measurement.domain.model.Measurement;
import java.time.Clock;
import java.time.Instant;

/**
 * Records a measurement produced by a device and publishes {@code MeasurementRecorded}.
 *
 * <p><strong>Transactional boundary:</strong> this use case performs a single write, so a
 * transaction would add nothing but an extra connection and is deliberately omitted.
 *
 * <p><strong>Device existence:</strong> the MER requires {@code deviceId} to be validated at
 * application level. The device module does not expose its public API yet, so the check is
 * deferred and documented; the schema already enforces the value as {@code VARCHAR(10)}.
 *
 * <p>This service contains no Spring annotations.
 */
public class RegisterMeasurementService implements RegisterMeasurement {

    private final MeasurementPersistencePort measurementPort;
    private final IdentifierGeneratorPort identifiers;
    private final MeasurementEventPort events;
    private final Clock clock;

    public RegisterMeasurementService(MeasurementPersistencePort measurementPort,
                                      IdentifierGeneratorPort identifiers,
                                      MeasurementEventPort events, Clock clock) {
        this.measurementPort = measurementPort;
        this.identifiers = identifiers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public MeasurementResult register(RegisterMeasurementCommand command) {
        Instant dateTime = command.dateTime() != null ? command.dateTime() : clock.instant();
        Measurement measurement = Measurement.record(identifiers.generate(), command.deviceId(),
                dateTime, command.voltage(), command.current(), command.activePower(),
                command.storedEnergy());
        measurementPort.save(measurement);

        // Observer pattern of the class diagram: future alert and recommendation modules
        // subscribe to this event instead of registering in-process observers.
        events.publish(new MeasurementRecorded(measurement.idMeasurement(), measurement.deviceId(),
                measurement.dateTime(), measurement.getVoltage(), measurement.getCurrent(),
                measurement.getActivePower(), measurement.getStoredEnergy()));

        return MeasurementResult.from(measurement);
    }
}
