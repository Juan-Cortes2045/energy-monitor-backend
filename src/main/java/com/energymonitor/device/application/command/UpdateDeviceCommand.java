package com.energymonitor.device.application.command;

/**
 * Edits the display data of a linked device.
 *
 * @param userId          the caller, who must be an OWNER of the home
 * @param applianceTypeId appliance the module measures
 * @param location        room, may be null
 */
public record UpdateDeviceCommand(String userId, String homeId, String deviceId, String name,
                                  String applianceTypeId, String location) {
}
