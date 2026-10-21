/**
 * Bounded context Security, as seen from this backend.
 *
 * <p>Identity, authentication, sessions, RBAC and the audit trail live in the security service
 * ({@code en-monitor-security-service}). What stays here is what this backend needs of it:
 * validating the access tokens it issues, the permission checks of the admin routes, and the
 * contracts in {@code api} through which other modules read profiles and administrators. They
 * are answered by the internal API of the service, through {@code adapter.out.remote}.
 *
 * <p>The rest of the system reads the caller's identity from the access token's subject, never
 * by calling this module.
 *
 * <p>Internal layout: {@code adapter}, {@code infrastructure}. Only the types exposed in
 * {@code api} are visible to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.security;

import org.springframework.modulith.ApplicationModule;
