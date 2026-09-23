/**
 * Bounded context Recommendations.
 *
 * <p>Generates suggestions from a home's own measured consumption and tracks
 * whether the user has read them. Reacts to {@code MeasurementRecorded}.
 */
@ApplicationModule(allowedDependencies = { "device", "measurement" })
package com.energymonitor.recommendation;

import org.springframework.modulith.ApplicationModule;