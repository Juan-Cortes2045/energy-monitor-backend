package com.energymonitor.device.application.port.out;

/**
 * Generates unique identifiers for device aggregates.
 */
public interface IdentifierGeneratorPort {

    String nextDeviceId();

    String nextDeviceStatusLogId();

    /**
     * A new device secret: the MQTT password of the device. Shown once, when the device is linked.
     */
    String nextApiKey();
}
