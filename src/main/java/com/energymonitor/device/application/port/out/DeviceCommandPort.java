package com.energymonitor.device.application.port.out;

/**
 * Commands to a module over its MQTT command topic ({@code energy-monitor/devices/{id}/command}).
 * They are retained, so a module that is off receives them when it connects again.
 */
public interface DeviceCommandPort {

    /** Tells the module it no longer belongs to a home: it forgets its key and offers Bluetooth. */
    void sendUnlinked(String deviceId);

    /** Removes any pending command, e.g. once the module is linked again. */
    void clearCommands(String deviceId);
}
