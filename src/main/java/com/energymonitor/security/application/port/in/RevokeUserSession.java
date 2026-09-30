package com.energymonitor.security.application.port.in;

import com.energymonitor.security.application.command.RevokeUserSessionCommand;

/**
 * Input port for revoking an authenticated session.
 */
public interface RevokeUserSession {

    /**
     * @param command the session to revoke
     */
    void revoke(RevokeUserSessionCommand command);
}