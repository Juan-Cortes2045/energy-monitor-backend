/**
 * Bounded context Security.
 *
 * <p>Owns identity, authentication, sessions, RBAC and the audit trail.
 * Upstream of every other module: the rest of the system reads the caller's
 * identity from the access token claims, never by calling this module. The one read
 * other modules may make is {@code UserProfileQuery} in {@code api}: the name and address of
 * accounts they already hold identifiers for, such as the members of a home.
 *
 * <p>Internal layout: {@code domain}, {@code application}, {@code adapter},
 * {@code infrastructure}. Only the types exposed in {@code api} are visible
 * to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.security;

import org.springframework.modulith.ApplicationModule;