package com.energymonitor.device.application.command;

/**
 * Links a physical module to a home, from the web pairing flow.
 *
 * @param userId          the caller, who must be an OWNER of the home
 * @param homeId          the home
 * @param deviceCode      code the module reports over Bluetooth (MQTT username)
 * @param name            display name
 * @param applianceTypeId appliance the module measures
 * @param location        room, may be null
 */
public record LinkDeviceCommand(String userId, String homeId, String deviceCode, String name,
                                String applianceTypeId, String location) {
}
