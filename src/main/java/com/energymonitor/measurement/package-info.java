/**
 * Bounded context Monitoring and Measurements.
 *
 * <p>Owns measurement ingestion, consumption history, statistics and
 * consumption levels. Publishes {@code MeasurementRecorded}.
 */
@ApplicationModule(allowedDependencies = { "device::api", "home::api" })
package com.energymonitor.measurement;

import org.springframework.modulith.ApplicationModule;