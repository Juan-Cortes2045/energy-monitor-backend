package com.energymonitor.alert.application.port.in;

import java.time.Instant;

/**
 * Use case: tell the members of a home that a module was linked to it.
 */
public interface RegisterDeviceLinkedAlert {

    void register(String deviceId, String homeId, Instant dateTime);
}
