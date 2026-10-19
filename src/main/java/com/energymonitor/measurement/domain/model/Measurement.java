package com.energymonitor.measurement.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root of a measurement: a single electrical reading produced by a device.
 *
 * <p>A measurement is immutable once recorded. There is no "consumption history" entity:
 * the history is the set of measurements of a device (domain diagram note).
 *
 * <p><strong>Device reference:</strong> {@code deviceId} references the {@code Device} of the
 * Devices bounded context. Per the MER it is validated at application level and generates
 * no SQL foreign key. It stays a plain {@code String} because {@code Device} cannot be
 * imported here; the existence check arrives when the device module exposes its public API.
 *
 * <p><strong>Observer pattern:</strong> the class diagram models {@code MeasurementObserver}
 * with {@code AlertObserver} and {@code RecommendationObserver} as subscribers. Those
 * subscribers live in their own bounded contexts, so the mechanism is realized as the
 * {@code MeasurementRecorded} application event published by
 * {@code RegisterMeasurementService}: future modules listen to the event instead of
 * registering in-process observers, which keeps the module dependency graph acyclic.
 *
 * <p>Invariants:
 * <ul>
 *   <li>MEAS-INV-001: {@code idMeasurement} is required, {@code VARCHAR(10)}</li>
 *   <li>MEAS-INV-002: {@code deviceId} is required, {@code VARCHAR(10)}</li>
 *   <li>MEAS-INV-003: {@code dateTime} is required</li>
 *   <li>MEAS-INV-004: {@code voltage >= 0}</li>
 *   <li>MEAS-INV-005: {@code current >= 0}</li>
 *   <li>MEAS-INV-006: {@code activePower >= 0}</li>
 *   <li>MEAS-INV-007: {@code storedEnergy >= 0}</li>
 * </ul>
 *
 * <p>Maps to the {@code measurement} table.
 */
public class Measurement {

    private final String idMeasurement;
    private final String deviceId;
    private final Instant dateTime;
    private final double voltage;
    private final double current;
    private final double activePower;
    private final double storedEnergy;

    /**
     * Rehydrates or records a measurement. Prefer {@link #record} for new readings.
     *
     * @param idMeasurement identifier, {@code VARCHAR(10)}
     * @param deviceId      identifier of the measuring device, {@code VARCHAR(10)}
     * @param dateTime      business timestamp of the reading
     * @param voltage       voltage reading, must not be negative (MEAS-INV-004)
     * @param current       current reading, must not be negative (MEAS-INV-005)
     * @param activePower   active power reading, must not be negative (MEAS-INV-006)
     * @param storedEnergy  accumulated energy reading, must not be negative (MEAS-INV-007)
     */
    public Measurement(String idMeasurement, String deviceId, Instant dateTime,
                       double voltage, double current, double activePower, double storedEnergy) {
        this.idMeasurement = Preconditions.text(idMeasurement, 10, "idMeasurement");
        this.deviceId = Preconditions.text(deviceId, 10, "deviceId");
        this.dateTime = Preconditions.notNull(dateTime, "dateTime");
        this.voltage = Preconditions.notNegative(voltage, "voltage");
        this.current = Preconditions.notNegative(current, "current");
        this.activePower = Preconditions.notNegative(activePower, "activePower");
        this.storedEnergy = Preconditions.notNegative(storedEnergy, "storedEnergy");
    }

    /**
     * Records a new measurement.
     *
     * @param idMeasurement identifier
     * @param deviceId      identifier of the measuring device
     * @param dateTime      business timestamp of the reading
     * @param voltage       voltage reading
     * @param current       current reading
     * @param activePower   active power reading
     * @param storedEnergy  accumulated energy reading
     * @return a new measurement
     */
    public static Measurement record(String idMeasurement, String deviceId, Instant dateTime,
                                     double voltage, double current, double activePower, double storedEnergy) {
        return new Measurement(idMeasurement, deviceId, dateTime, voltage, current, activePower, storedEnergy);
    }

    /** @return the identifier */
    public String idMeasurement() {
        return idMeasurement;
    }

    /** @return identifier of the measuring device */
    public String deviceId() {
        return deviceId;
    }

    /** @return business timestamp of the reading */
    public Instant dateTime() {
        return dateTime;
    }

    /** @return the voltage reading */
    public double getVoltage() {
        return voltage;
    }

    /** @return the current reading */
    public double getCurrent() {
        return current;
    }

    /** @return the active power reading */
    public double getActivePower() {
        return activePower;
    }

    /** @return the accumulated energy reading */
    public double getStoredEnergy() {
        return storedEnergy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Measurement that)) {
            return false;
        }
        return idMeasurement.equals(that.idMeasurement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMeasurement);
    }

    @Override
    public String toString() {
        return "Measurement{idMeasurement='" + idMeasurement + "', deviceId='" + deviceId + "', dateTime=" + dateTime + ", activePower=" + activePower + "}";
    }
}
