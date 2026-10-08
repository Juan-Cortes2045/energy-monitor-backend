package com.energymonitor.measurement.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * What the measurement module needs to know about homes and their devices.
 */
public interface HomeAccessPort {

    /** @param limitPeriod {@code DAILY} or {@code MONTHLY}: the limit the owner set */
    record Limits(double dailyLimit, double monthlyLimit, String limitPeriod) {
    }

    boolean isMember(String userId, String homeId);

    /** Identifiers of the devices currently linked to the home. */
    List<String> deviceIds(String homeId);

    /** The home a device is linked to, if any. */
    Optional<String> homeOf(String deviceId);

    Optional<Limits> limits(String homeId);
}
