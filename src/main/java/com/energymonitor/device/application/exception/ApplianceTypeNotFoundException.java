package com.energymonitor.device.application.exception;

public class ApplianceTypeNotFoundException extends DeviceApplicationException {

    public ApplianceTypeNotFoundException(String applianceTypeId) {
        super("appliance type not found: " + applianceTypeId);
    }
}
