/**
 * Bounded context Recommendations.
 *
 * <p>Turns the observed consumption of a home into suggestions: shift usage out of the evening
 * peak, cut standby, watch a device that consumes more than before, and the home's pace against
 * its limit. The rules run hourly over the hourly energy {@code measurement::api} computes; the
 * devices, members and limits come from {@code device::api} and {@code home::api}.
 *
 * <p>Each new recommendation is published as {@code RecommendationCreated}
 * ({@code recommendation::api}); the notification module delivers it by mail and push.
 */
@ApplicationModule(allowedDependencies = { "device :: api", "measurement :: api", "home :: api" })
package com.energymonitor.recommendation;

import org.springframework.modulith.ApplicationModule;
