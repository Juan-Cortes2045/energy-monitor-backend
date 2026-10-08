package com.energymonitor.device.application.command;

public record UnlinkDeviceCommand(String userId, String homeId, String deviceId) {
}
