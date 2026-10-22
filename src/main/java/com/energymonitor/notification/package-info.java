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
 * <p><strong>What this module takes from the security service.</strong> The verification and
 * recovery codes are generated, hashed and stored by the security service; it hands them over
 * the message broker, and this module mails them ({@code MailRequestsConsumer}) and keeps no
 * copy. Its account events become administrator notifications, and the names and addresses of
 * accounts are read through {@code security::api}, the only part of security this module may
 * see.
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
