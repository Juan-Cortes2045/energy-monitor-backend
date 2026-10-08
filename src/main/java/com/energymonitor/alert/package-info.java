/**
 * Bounded context Alerts.
 *
 * <p>Raises and tracks threshold and connectivity alerts. Reacts to
 * {@code MeasurementRecorded} and {@code DeviceConnectivityLost}.
 */
@ApplicationModule(allowedDependencies = { "home :: api", "device :: api", "measurement :: api" })
package com.energymonitor.alert;

import org.springframework.modulith.ApplicationModule;