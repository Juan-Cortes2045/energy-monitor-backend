/**
 * Public API of the Security bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code security::api} instead
 * of {@code security} directly.
 *
 * <p>It holds the contracts other modules are meant to satisfy or call, and nothing else. The
 * output ports that only this module implements stay in {@code application.port.out}, where
 * they are invisible from outside.
 */
@NamedInterface
package com.energymonitor.security.api;

import org.springframework.modulith.NamedInterface;
