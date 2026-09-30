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
 * Ends an authenticated session and records the event in the audit trail.
 *
 * <p>This use case backs {@code POST /auth/logout}, so it is the ordinary end of a session and
 * it closes rather than revokes. The domain separates the two events deliberately: closing is
 * the holder walking away, revoking is a security invalidation, and collapsing them would make
 * a logout indistinguishable from a compromise when the row is read back during an incident.
 *
 * <p><strong>Naming debt.</strong> The type, the port and the command are still called
 * {@code RevokeUserSession}, which no longer describes what they do. Renaming them would reach
 * {@code AuthController}, which injects the port, and renaming the port is out of scope for the
 * session capability work. Until the controller can move too, the misleading name is kept and
 * this paragraph is the correction.
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
        // Logout is an ordinary termination, so the session is closed. It stays unrevoked, which
        // is the whole point of the distinction: this is not a security event.
        session.close(now);
        sessionPort.save(session);
        auditLogPort.save(new AuditLog(identifiers.generate(), session.idUser(), AuditAction.LOGOUT,
                null, command.ipAddress(), null, now));
    }
}