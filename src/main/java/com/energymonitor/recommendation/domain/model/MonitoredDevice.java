package com.energymonitor.recommendation.domain.model;

/**
 * A device of the home as the rules see it.
 *
 * @param alwaysOn the appliance is expected to run all night (a refrigerator), so drawing power
 *                 at night is not standby waste
 */
public record MonitoredDevice(String deviceId, boolean alwaysOn) {
}
