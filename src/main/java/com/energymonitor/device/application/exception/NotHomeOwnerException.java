package com.energymonitor.device.application.exception;

public class NotHomeOwnerException extends DeviceApplicationException {

    public NotHomeOwnerException(String homeId) {
        super("only an owner of home " + homeId + " can manage its devices");
    }
}
