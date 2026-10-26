/**
 * Bounded context Security, as seen from this backend.
 *
 * <p>Identity, authentication, sessions, RBAC and the audit trail live in the security service
 * ({@code en-monitor-security-service}). What stays here is what this backend needs of it:
 * validating the access tokens it issues, and the CORS policy of the API.
 *
 * <p>The rest of the system reads the caller's identity from the access token's subject, never
 * by calling this module.
 *
 * <p>Internal layout: {@code infrastructure}. No other module depends on it.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.security;

import org.springframework.modulith.ApplicationModule;
