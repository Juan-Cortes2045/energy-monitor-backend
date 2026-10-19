/**
 * Public API of the Devices bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code device::api} instead
 * of {@code device} directly.
 */
@NamedInterface
package com.energymonitor.device.api;

import org.springframework.modulith.NamedInterface;
