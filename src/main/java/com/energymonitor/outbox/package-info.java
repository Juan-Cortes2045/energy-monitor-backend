/**
 * Outbox: the events this backend announces to other services over the message broker.
 *
 * <p>A module writes an event with {@code outbox::api} when it stores the change the event is
 * about; a relay publishes the pending events afterwards, oldest first, and marks each one only
 * once the broker confirmed it and a queue took it. A broker or a consumer that is down therefore
 * delays an event but does not lose it.
 *
 * <p>Today the recommendations ({@code recommendation.created}), which the notification service
 * delivers to the members of their home. The alerts are announced by the alert service.
 *
 * <p>Internal layout: {@code adapter}, {@code infrastructure}. Only the types exposed in
 * {@code api} are visible to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.outbox;

import org.springframework.modulith.ApplicationModule;
