/**
 * Public API of the Security bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code security::api} instead
 * of {@code security} directly.
 *
 * <p>It holds the queries other modules call, answered by the security service, and the account
 * events of that service as other modules receive them from the message broker.
 */
@NamedInterface
package com.energymonitor.security.api;

import org.springframework.modulith.NamedInterface;
