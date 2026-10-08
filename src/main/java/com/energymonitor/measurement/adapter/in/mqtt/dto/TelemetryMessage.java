package com.energymonitor.measurement.adapter.in.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TelemetryMessage(
        @JsonProperty("deviceId") String deviceId,
        @JsonProperty("dateTime") Instant dateTime,
        @JsonProperty("voltage") Double voltage,
        @JsonProperty("current") Double current,
        @JsonProperty("activePower") Double activePower,
        @JsonProperty("storedEnergy") Double storedEnergy,
        @JsonProperty("frequency") Integer frequency,
        @JsonProperty("powerFactor") Double powerFactor,
        @JsonProperty("rssi") Integer rssi,
        @JsonProperty("sequence") Long sequence
) {
}
