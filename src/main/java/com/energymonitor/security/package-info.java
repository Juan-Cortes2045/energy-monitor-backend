/**
 * Bounded context Security.
 *
 * <p>Owns identity, authentication, sessions, RBAC and the audit trail.
 * Upstream of every other module: the rest of the system reads the caller's
 * identity from the access token claims, never by calling this module.
 *
 * <p>Internal layout: {@code domain}, {@code application}, {@code adapter},
 * {@code infrastructure}. Only the types exposed in {@code api} are visible
 * to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.security;

import org.springframework.modulith.ApplicationModule;