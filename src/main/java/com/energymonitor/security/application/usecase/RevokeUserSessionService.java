package com.energymonitor.security.application.usecase;

import com.energymonitor.security.application.command.RevokeUserSessionCommand;
import com.energymonitor.security.application.exception.SessionNotFoundException;
import com.energymonitor.security.application.port.in.RevokeUserSession;
import com.energymonitor.security.application.port.out.AuditLogPersistencePort;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.UserSessionPersistencePort;
import com.energymonitor.security.domain.model.AuditAction;
import com.energymonitor.security.domain.model.AuditLog;
import com.energymonitor.security.domain.model.UserSession;
import java.time.Clock;
import java.time.Instant;

/**
 * Revokes an authenticated session and records the closure.
 */
public class RevokeUserSessionService implements RevokeUserSession {

    private final UserSessionPersistencePort sessionPort;
    private final AuditLogPersistencePort auditLogPort;
    private final IdentifierGeneratorPort identifiers;
    private final Clock clock;

    public RevokeUserSessionService(UserSessionPersistencePort sessionPort,
                                    AuditLogPersistencePort auditLogPort,
                                    IdentifierGeneratorPort identifiers, Clock clock) {
        this.sessionPort = sessionPort;
        this.auditLogPort = auditLogPort;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    @Override
    public void revoke(RevokeUserSessionCommand command) {
        Instant now = clock.instant();
        UserSession session = sessionPort.findActive(command.idUserSession())
                .orElseThrow(() -> new SessionNotFoundException("no active session " + command.idUserSession()));
        session.revoke();
        sessionPort.save(session);
        auditLogPort.save(new AuditLog(identifiers.generate(), session.idUser(), AuditAction.LOGOUT,
                null, command.ipAddress(), null, now));
    }
}