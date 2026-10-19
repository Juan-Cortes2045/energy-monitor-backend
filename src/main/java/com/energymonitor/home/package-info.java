/**
 * Bounded context Home Management.
 *
 * <p>Owns homes, membership, home-scoped roles and consumption thresholds.
 *
 * <p>Depends on {@code security::api} only to show who the members and the owner of a home are:
 * this module stores user identifiers, never names or addresses.
 */
@ApplicationModule(allowedDependencies = {"security::api"})
package com.energymonitor.home;

import org.springframework.modulith.ApplicationModule;