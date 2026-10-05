package com.energymonitor.measurement.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA mapping of the {@code measurement} table.
 *
 * <p>{@code device_id} has no SQL foreign key: it references the Devices bounded context
 * at application level, per the MER conventions.
 */
@Entity
@Table(name = "measurement")
public class MeasurementEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_measurement", nullable = false, length = 10)
    private String idMeasurement;

    @Column(name = "device_id", nullable = false, length = 10)
    private String deviceId;

    @Column(name = "date_time", nullable = false)
    private Instant dateTime;

    @Column(name = "voltage", nullable = false)
    private double voltage;

    @Column(name = "current", nullable = false)
    private double current;

    @Column(name = "active_power", nullable = false)
    private double activePower;

    @Column(name = "stored_energy", nullable = false)
    private double storedEnergy;

    public MeasurementEntity() {
    }

    public MeasurementEntity(String idMeasurement, String deviceId, Instant dateTime,
                             double voltage, double current, double activePower, double storedEnergy) {
        this.idMeasurement = idMeasurement;
        this.deviceId = deviceId;
        this.dateTime = dateTime;
        this.voltage = voltage;
        this.current = current;
        this.activePower = activePower;
        this.storedEnergy = storedEnergy;
    }

    public String getIdMeasurement() {
        return idMeasurement;
    }

    public void setIdMeasurement(String idMeasurement) {
        this.idMeasurement = idMeasurement;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Instant getDateTime() {
        return dateTime;
    }

    public void setDateTime(Instant dateTime) {
        this.dateTime = dateTime;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getActivePower() {
        return activePower;
    }

    public void setActivePower(double activePower) {
        this.activePower = activePower;
    }

    public double getStoredEnergy() {
        return storedEnergy;
    }

    public void setStoredEnergy(double storedEnergy) {
        this.storedEnergy = storedEnergy;
    }
}
