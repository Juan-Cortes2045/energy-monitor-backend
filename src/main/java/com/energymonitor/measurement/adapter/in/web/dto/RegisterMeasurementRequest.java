package com.energymonitor.measurement.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request DTO for recording a measurement.
 *
 * @param deviceId     identifier of the measuring device, max 10 characters
 * @param dateTime     business timestamp of the reading; the server clock is used when absent
 * @param voltage      voltage reading, must not be negative
 * @param current      current reading, must not be negative
 * @param activePower  active power reading, must not be negative
 * @param storedEnergy accumulated energy reading, must not be negative
 */
public record RegisterMeasurementRequest(
        @NotBlank @Size(max = 10) String deviceId,
        Instant dateTime,
        @NotNull @PositiveOrZero Double voltage,
        @NotNull @PositiveOrZero Double current,
        @NotNull @PositiveOrZero Double activePower,
        @NotNull @PositiveOrZero Double storedEnergy
) {
}
