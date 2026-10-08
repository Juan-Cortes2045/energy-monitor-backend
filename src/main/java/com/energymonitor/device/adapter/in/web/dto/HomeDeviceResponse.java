package com.energymonitor.device.adapter.in.web.dto;

import com.energymonitor.device.application.result.HomeDeviceResult;
import java.time.Instant;

/**
 * A device of a home. {@code status} is {@code ONLINE}, {@code OFFLINE}, or {@code null} when the
 * module has never reported; {@code signalStrength} is the last RSSI in dBm.
 */
public record HomeDeviceResponse(String idDevice, String name, String applianceTypeId, String applianceType,
                                 String location, Instant installationDate, String deviceCode,
                                 String status, Integer signalStrength, Instant lastSeen) {

    public static HomeDeviceResponse from(HomeDeviceResult r) {
        return new HomeDeviceResponse(r.idDevice(), r.name(), r.applianceTypeId(), r.applianceType(),
                r.location(), r.installationDate(), r.deviceCode(), r.status(), r.signalStrength(), r.lastSeen());
    }
}
