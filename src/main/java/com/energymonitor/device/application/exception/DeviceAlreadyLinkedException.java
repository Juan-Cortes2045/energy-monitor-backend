package com.energymonitor.device.application.exception;

public class DeviceAlreadyLinkedException extends DeviceApplicationException {

    public DeviceAlreadyLinkedException(String deviceCode) {
        super("device " + deviceCode + " is linked to another home");
    }
}
