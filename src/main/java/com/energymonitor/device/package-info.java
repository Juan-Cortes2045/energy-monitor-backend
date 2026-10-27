/**
 * Bounded context Devices, as seen from this backend.
 *
 * <p>The device registry, the device credentials, the broker's authentication, the commands to
 * the modules and their connectivity live in the device service ({@code en-monitor-device-service}).
 * What stays here is what this backend needs of it: reading devices through its internal API
 * ({@code adapter.out.remote}, cached), and hearing its links and unlinks
 * ({@code adapter.in.messaging}), which drop what that cache keeps of a device. The connectivity
 * events go to the alert service.
 *
 * <p>Internal layout: {@code adapter}, {@code infrastructure}. Only the types exposed in
 * {@code api} are visible to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.device;

import org.springframework.modulith.ApplicationModule;
