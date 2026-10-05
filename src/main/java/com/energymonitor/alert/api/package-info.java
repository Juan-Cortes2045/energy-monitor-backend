/**
 * Public API of the Alerts bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code alert::api}
 * instead of {@code alert} directly.
 *
 * <p>The package carries the integration surface the {@code notification} module
 * needs: the {@code AlertRaised} event it will subscribe to and the enums that
 * ride inside it.
 */
@NamedInterface
package com.energymonitor.alert.api;

import org.springframework.modulith.NamedInterface;
