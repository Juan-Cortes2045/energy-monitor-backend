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
 * <p>The use case is named after revocation and the domain offers both {@code revoke} and
 * {@code close}, so the distinction is worth stating: revocation is the security-driven
 * invalidation of a credential, whereas closing is the ordinary end of a session. Logout is
 * the ordinary case, so once the session capability work lands this use case is expected to
 * close the session rather than revoke it, and administrative revocation becomes a separate
 * concern.
 *
 * <p>Until then the session is revoked, which is the conservative direction: a revoked session
 * is terminal and cannot be resumed, so the interim behaviour cannot leave a usable session
 * behind. What it does lose is the ability to tell a logout from a compromise when reading the
 * row later.
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