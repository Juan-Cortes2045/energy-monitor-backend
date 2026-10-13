package com.energymonitor.device.application.port.in;

/**
 * Use case: the home was deleted, so every module linked to it is unlinked. Triggered by the
 * system ({@code HomeDeleted}), not by a user, so no role is checked here: the home module
 * already allowed the deletion.
 */
public interface UnlinkHomeDevices {

    /** @return how many modules were unlinked */
    int unlinkAll(String homeId);
}
