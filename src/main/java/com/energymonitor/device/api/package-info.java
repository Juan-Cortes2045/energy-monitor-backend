/**
 * Public API of the Devices bounded context, as this backend sees it.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}: {@link com.energymonitor.device.api.DeviceApi} to read devices.
 */
@NamedInterface
package com.energymonitor.device.api;

import org.springframework.modulith.NamedInterface;
