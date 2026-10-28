/**
 * Bounded context Home Management, as seen from this backend.
 *
 * <p>Homes, their members, their roles, ownership transfer, the consumption thresholds and the
 * home types live in the home service ({@code en-monitor-home-service}), which also announces
 * deleted homes and cleans up after deleted accounts. What stays here is what this backend needs
 * of it: reading a home, its members and its limits through its internal API
 * ({@code adapter.out.remote}, cached), for the recommendations.
 *
 * <p>Internal layout: {@code adapter}, {@code infrastructure}. Only the types exposed in
 * {@code api} are visible to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.home;

import org.springframework.modulith.ApplicationModule;
