package com.energymonitor.device.application.port.out;

/**
 * Port to validate home existence from other bounded contexts.
 */
public interface HomeLookupPort {

    boolean homeExists(String homeId);

    boolean isMember(String userId, String homeId);

    boolean isOwner(String userId, String homeId);
}
