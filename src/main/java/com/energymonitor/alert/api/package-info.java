/**
 * Public API of the Alerts bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code alert::api}
 * instead of {@code alert} directly.
 *
 * <p>The package carries the {@code AlertRaised} event, which the alert module announces to
 * the notification service through the outbox ({@code alert.raised}), and the enums that ride
 * inside it.
 */
@NamedInterface
package com.energymonitor.alert.api;

import org.springframework.modulith.NamedInterface;
