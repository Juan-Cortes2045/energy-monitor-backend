/**
 * Public API of the Home Management bounded context, as this backend sees it.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}: {@link com.energymonitor.home.api.HomeApi} to read a home, its members
 * and its limits, answered by the home service.
 */
@NamedInterface
package com.energymonitor.home.api;

import org.springframework.modulith.NamedInterface;
