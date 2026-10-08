/**
 * Bounded context Devices.
 *
 * <p>Owns the device registry, device authentication by API key and
 * connectivity tracking. Publishes {@code DeviceConnectivityLost}.
 */
@ApplicationModule(allowedDependencies = { "home::api" })
package com.energymonitor.device;

import org.springframework.modulith.ApplicationModule;