/**
 * Public API of the Home Management bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code home::api} instead
 * of {@code home} directly.
 */
@NamedInterface
package com.energymonitor.home.api;

import org.springframework.modulith.NamedInterface;
