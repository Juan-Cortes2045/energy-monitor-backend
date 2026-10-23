/**
 * Public API of the Devices bounded context, as this backend sees it.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}: {@link com.energymonitor.device.api.DeviceApi} to read devices, and
 * the connectivity events the alert module listens to, which arrive from the device service.
 */
@NamedInterface
package com.energymonitor.device.api;

import org.springframework.modulith.NamedInterface;
