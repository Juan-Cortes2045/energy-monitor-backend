/**
 * Bounded context Notification.
 *
 * <p>Owns the delivery log: a record that an outbound message was attempted, over which channel,
 * and whether it succeeded. It answers a question no other module can. An alert and a
 * recommendation each keep their own lifecycle in their own module; neither of them knows whether
 * a message about it ever reached a person.
 *
 * <p><strong>The message body is never stored.</strong> A password recovery secret travels in that
 * body, so a column holding it in clear text would undo the decision to keep only a hash
 * elsewhere. Only the recipient, the type, the subject and the outcome are kept.
 *
 * <p>Internal layout: {@code domain}, {@code application}, {@code adapter},
 * {@code infrastructure}. Only the types exposed in {@code api} are visible to other modules.
 *
 * <p><strong>Why this module depends on {@code security::api}.</strong> The recovery secret is
 * generated, hashed and stored by the security module, and it exists in clear text only inside
 * that module. Delivering it is therefore an outbound adapter <em>of</em> security: this module
 * implements security's own {@code PasswordResetDeliveryPort} and calls nothing else of it.
 *
 * <p>The dependency is declared against {@code security::api} rather than {@code security}
 * because that named interface is the only part of security this module is allowed to see. The
 * direction matters: security knows nothing about mail, SMTP or message wording, so choosing a
 * different channel replaces one adapter in this module and leaves security untouched. Widening
 * the allowance to the whole module would let mail code reach into security's internals, which
 * is the coupling this arrangement exists to prevent.
 *
 * <p><strong>Alert delivery.</strong> Every {@code AlertRaised} of {@code alert::api} is sent to
 * the members of its home ({@code home::api}), by mail and Web Push according to each member's
 * preferences; the names in the message come from {@code home::api} and {@code device::api}.
 * Every {@code RecommendationCreated} of {@code recommendation::api} is delivered the same way.
 */
@ApplicationModule(allowedDependencies = {"security::api", "home::api", "device::api", "alert::api",
        "recommendation::api"})
package com.energymonitor.notification;

import org.springframework.modulith.ApplicationModule;
