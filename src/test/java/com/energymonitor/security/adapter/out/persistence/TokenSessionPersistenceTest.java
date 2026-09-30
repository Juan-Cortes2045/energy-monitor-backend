package com.energymonitor.security.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.security.adapter.out.persistence.support.Instants;
import com.energymonitor.security.domain.model.PasswordResetToken;
import com.energymonitor.security.domain.model.RevocationReason;
import com.energymonitor.security.domain.model.UserSession;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Round trips of the time-boxed credentials: password reset tokens and user sessions.
 *
 * <p>The lifecycle flags are the security-sensitive part: {@code used} on a token and
 * {@code revoked}/{@code closedAt} on a session must come back exactly as persisted, or a
 * consumed credential would rehydrate as usable.
 */
@SpringBootTest
@Transactional
class TokenSessionPersistenceTest {

    private static final String USER_ID = "use0000001";
    private static final String TOKEN_ID = "tok0000001";
    private static final String SESSION_ID = "ses0000001";
    private static final String RESET_TOKEN = "reset-value-1234567890";
    private static final String REFRESH_TOKEN = "refresh-value-1234567890";
    private static final Instant OPENED_AT = Instant.parse("2026-02-01T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-03-01T08:00:00Z");
    private static final Instant CLOSED_AT = Instant.parse("2026-02-02T09:30:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PersonPersistenceAdapter persons;

    @Autowired
    private UserPersistenceAdapter users;

    @Autowired
    private PasswordResetTokenPersistenceAdapter passwordResetTokens;

    @Autowired
    private UserSessionPersistenceAdapter userSessions;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void tokenUsedFlagSurvivesInsertUpdateAndReload() {
        PersistenceFixtures.seedUser(persons, users);
        passwordResetTokens.save(new PasswordResetToken(TOKEN_ID, USER_ID, RESET_TOKEN,
                OPENED_AT, EXPIRES_AT, false));
        flushAndClear();

        PasswordResetToken read = passwordResetTokens.findActiveByValue(RESET_TOKEN).orElseThrow();
        assertEquals(TOKEN_ID, read.idResetToken());
        assertEquals(USER_ID, read.idUser());
        assertEquals(OPENED_AT, read.createdAt());
        assertEquals(EXPIRES_AT, read.expirationAt());
        assertFalse(read.isUsed());

        PasswordResetToken consumed = new PasswordResetToken(TOKEN_ID, USER_ID, RESET_TOKEN,
                OPENED_AT, EXPIRES_AT, true);
        passwordResetTokens.update(consumed);
        flushAndClear();

        PasswordResetToken stored = passwordResetTokens.findActive(TOKEN_ID).orElseThrow();
        assertTrue(stored.isUsed());
        assertEquals(OPENED_AT, stored.createdAt());
    }

    @Test
    void openSessionRoundTripsAndKeepAllFieldsOnReload() {
        PersistenceFixtures.seedUser(persons, users);
        userSessions.save(UserSession.open(SESSION_ID, USER_ID, OPENED_AT, EXPIRES_AT, null, null));
        flushAndClear();

        UserSession read = userSessions.findActive(SESSION_ID).orElseThrow();
        assertEquals(SESSION_ID, read.idUserSession());
        assertEquals(USER_ID, read.idUser());
        assertEquals(OPENED_AT, read.createdAt());
        assertEquals(EXPIRES_AT, read.expirationAt());
        assertTrue(read.ipAddress().isEmpty());
        assertTrue(read.userAgent().isEmpty());
        assertFalse(read.isRevoked());
        assertTrue(read.closedAt().isEmpty());
    }

    @Test
    void closedSessionKeepsRevokedFlagAndClosureInstant() {
        PersistenceFixtures.seedUser(persons, users);
        UserSession session = UserSession.open(SESSION_ID, USER_ID,
                OPENED_AT, EXPIRES_AT, "192.168.1.10", "test-agent");
        session.revoke(OPENED_AT.plusSeconds(120), RevocationReason.PASSWORD_CHANGED);
        session.close(CLOSED_AT);
        userSessions.save(session);
        flushAndClear();

        UserSession read = userSessions.findActive(SESSION_ID).orElseThrow();
        assertTrue(read.isRevoked());
        assertEquals(CLOSED_AT, read.closedAt().orElseThrow());
        assertEquals("192.168.1.10", read.ipAddress().orElseThrow());
        assertEquals("test-agent", read.userAgent().orElseThrow());
        assertEquals(OPENED_AT, read.createdAt());
    }

    @Test
    void revokedSessionRoundTripsInstantAndReason() {
        PersistenceFixtures.seedUser(persons, users);
        UserSession session = UserSession.open(SESSION_ID, USER_ID,
                OPENED_AT, EXPIRES_AT, "192.168.1.10", "test-agent");
        Instant revokedAt = OPENED_AT.plusSeconds(300);
        session.revoke(revokedAt, RevocationReason.REFRESH_TOKEN_REUSE);
        userSessions.save(session);
        flushAndClear();

        UserSession read = userSessions.findActive(SESSION_ID).orElseThrow();
        assertTrue(read.isRevoked());
        assertEquals(revokedAt, read.revokedAt().orElseThrow());
        assertEquals(RevocationReason.REFRESH_TOKEN_REUSE, read.revokedReason().orElseThrow());
        // Revoking is not closing: the two stay distinguishable in the row.
        assertTrue(read.closedAt().isEmpty());
    }

    @Test
    void rotatedSessionKeepsItsIdentityAndSlidesOnlyTheWindow() {
        PersistenceFixtures.seedUser(persons, users);
        UserSession session = UserSession.open(SESSION_ID, USER_ID,
                OPENED_AT, EXPIRES_AT, "192.168.1.10", "test-agent");
        Instant rotatedAt = OPENED_AT.plusSeconds(3600);
        Instant newExpiration = EXPIRES_AT.plus(Duration.ofDays(7));
        session.rotate(newExpiration, rotatedAt);
        userSessions.save(session);
        flushAndClear();

        UserSession read = userSessions.findActive(SESSION_ID).orElseThrow();
        assertEquals(Instants.truncate(newExpiration), read.expirationAt());
        // Rotation slides the login, never replaces it: no second session is created.
        assertEquals(SESSION_ID, read.idUserSession());
        assertEquals(USER_ID, read.idUser());
        assertEquals(OPENED_AT, read.createdAt());
        assertTrue(read.isActive(rotatedAt));
    }
}