package com.energymonitor.security.application.command;

/**
 * Input of {@code RevokeUserSession}: terminates an authenticated session.
 *
 * @param idUserSession the session to revoke
 * @param idUser        the account the session belongs to, kept for the audit trail
 * @param ipAddress     optional origin of the request, recorded in the audit trail
 */
public record RevokeUserSessionCommand(String idUserSession, String idUser, String ipAddress) {
}