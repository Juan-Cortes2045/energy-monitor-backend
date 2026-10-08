package com.energymonitor.device.application.port.in;

/**
 * Use case: decide who may connect to the MQTT broker and what each client may do there.
 *
 * <p>The broker delegates both questions to the backend, so the device registry is the only
 * source of truth for device credentials: registering a device is enough for it to connect.
 */
public interface AuthorizeBrokerAccess {

    /**
     * The kind of access the broker is asking about.
     */
    enum Access {
        READ,
        WRITE,
        SUBSCRIBE
    }

    boolean authenticate(String username, String password);

    boolean authorize(String username, String topic, Access access);
}
