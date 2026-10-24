package com.energymonitor.home.adapter.out.messaging;

/**
 * The messages this module publishes, named by their routing key on the
 * {@code energy-monitor.home} topic exchange.
 *
 * <p>These names are a contract with the services that consume them: renaming one silently
 * stops its delivery, because the consumer's binding no longer matches.
 */
public final class HomeMessageTypes {

    /** A home was deleted: the device service unlinks its modules. */
    public static final String HOME_DELETED = "home.deleted";

    private HomeMessageTypes() {
    }
}
