package com.energymonitor.security.application.port.out;

import com.energymonitor.security.domain.model.Email;
import java.time.Instant;

/**
 * Output port for getting a password-reset secret to its owner. This is the extension point
 * the recovery flow waits on.
 *
 * <p>The secret exists in the clear in exactly two moments: while it is being generated, and
 * while it is on its way to the person who asked for a reset. Between them it is a hash. That
 * is why the reset endpoint cannot return it and why this port exists instead: delivery is the
 * only way the value legitimately leaves the server, so the value is handed to an outbound
 * adapter here and never to a response body.
 *
 * <p>An implementation is a client of whatever channel the deployment uses - a mail gateway, an
 * SMS provider, a push service - and it is the only place where the message content and the
 * reset link format are decided. The application layer states what has to be delivered and to
 * whom, and nothing about how.
 *
 * <p><strong>Implementations must not throw a distinguishable error.</strong> The endpoint that
 * triggers delivery answers 202 whether or not the account exists, so an implementation that
 * lets a transport failure escape would turn a delivery outage into a way of learning which
 * addresses are registered. The use case therefore treats a failure here as undeliverable and
 * keeps the acknowledgement neutral.
 */
public interface PasswordResetDeliveryPort {

    /**
     * Delivers a reset secret to the account that requested it.
     *
     * @param recipient  the address the recovery belongs to
     * @param clearToken the secret in clear text, valid until {@code validUntil}
     * @param validUntil the instant the secret stops being redeemable
     */
    void deliver(Email recipient, String clearToken, Instant validUntil);
}
