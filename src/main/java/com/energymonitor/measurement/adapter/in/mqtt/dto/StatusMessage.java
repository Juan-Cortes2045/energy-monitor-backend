package com.energymonitor.measurement.adapter.in.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatusMessage(
        @JsonProperty("deviceId") String deviceId,
        @JsonProperty("deviceCode") String deviceCode,
        @JsonProperty("deviceName") String deviceName,
        @JsonProperty("location") String location,
        @JsonProperty("status") String status,
        @JsonProperty("rssi") Integer rssi,
        @JsonProperty("ip") String ip,
        @JsonProperty("firmwareVersion") String firmwareVersion,
        @JsonProperty("uptimeSeconds") Long uptimeSeconds
) {
}
