package com.energymonitor.security.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserSession")
class UserSessionTest {

    private static final String ID = "USS0000001";
    private static final String ID_USER = "USR0000001";
    private static final Instant CREATED_AT = Instant.parse("2026-04-01T07:00:00Z");
    private static final Instant EXPIRATION_AT = Instant.parse("2026-04-01T15:00:00Z");
    private static final Instant ONE_HOUR_LATER = CREATED_AT.plusSeconds(3600);

    private static UserSession openSession() {
        return UserSession.open(ID, ID_USER, "refresh-token-opaco", CREATED_AT, EXPIRATION_AT,
                "192.168.1.10", "Mozilla/5.0");
    }

    @Nested
    @DisplayName("fresh session")
    class Fresh {

        @Test
        @DisplayName("is active, not revoked and not closed")
        void isActiveWhenOpen() {
            UserSession session = openSession();

            assertThat(session.isActive(CREATED_AT)).isTrue();
            assertThat(session.isRevoked()).isFalse();
            assertThat(session.closedAt()).isEmpty();
        }

        @Test
        @DisplayName("keeps its client data")
        void keepsClientData() {
            UserSession session = openSession();

            assertThat(session.idUser()).isEqualTo(ID_USER);
            assertThat(session.refreshToken()).isEqualTo("refresh-token-opaco");
            assertThat(session.ipAddress()).contains("192.168.1.10");
            assertThat(session.userAgent()).contains("Mozilla/5.0");
        }
    }

    @Nested
    @DisplayName("expiration")
    class Expiration {

        @Test
        @DisplayName("is active one second before the expiration")
        void activeJustBeforeExpiration() {
            assertThat(openSession().isActive(EXPIRATION_AT.minusSeconds(1))).isTrue();
        }

        @Test
        @DisplayName("is not active once the expiration is reached")
        void inactiveAtExpiration() {
            UserSession session = openSession();

            assertThat(session.isExpired(EXPIRATION_AT)).isTrue();
            assertThat(session.isActive(EXPIRATION_AT)).isFalse();
        }
    }

    @Nested
    @DisplayName("revocation")
    class Revocation {

        @Test
        @DisplayName("a revoked session is never valid")
        void revokedSessionIsNotActive() {
            UserSession session = openSession();

            session.revoke();

            assertThat(session.isRevoked()).isTrue();
            assertThat(session.isActive(CREATED_AT)).isFalse();
        }

        @Test
        @DisplayName("revoking twice keeps it revoked")
        void revokeIsIdempotent() {
            UserSession session = openSession();

            session.revoke();
            session.revoke();

            assertThat(session.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("closure")
    class Closure {

        @Test
        @DisplayName("closing stamps the instant and ends the session")
        void closeStampsInstant() {
            UserSession session = openSession();
            Instant closedAt = Instant.parse("2026-04-01T09:00:00Z");

            session.close(closedAt);

            assertThat(session.closedAt()).contains(closedAt);
            assertThat(session.isActive(closedAt)).isFalse();
        }

        @Test
        @DisplayName("a closed session stays invalid even before its expiration")
        void closedSessionIsNotActive() {
            UserSession session = openSession();
            session.close(CREATED_AT.plusSeconds(60));

            assertThat(session.isActive(ONE_HOUR_LATER)).isFalse();
        }
    }

    @Test
    @DisplayName("refuses an expiration that is not after the creation")
    void refusesInvertedWindow() {
        assertThatThrownBy(() -> UserSession.open(ID, ID_USER, "token", EXPIRATION_AT, CREATED_AT, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expirationAt must be after createdAt");
    }

    @Test
    @DisplayName("refuses a window whose ends coincide")
    void refusesEmptyWindow() {
        assertThatThrownBy(() -> UserSession.open(ID, ID_USER, "token", CREATED_AT, CREATED_AT, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expirationAt must be after createdAt");
    }

    @Test
    @DisplayName("rehydrating enforces the same window as opening")
    void rehydrationEnforcesTheSameWindow() {
        assertThatThrownBy(() -> new UserSession(ID, ID_USER, "token", EXPIRATION_AT, CREATED_AT,
                null, null, false, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expirationAt must be after createdAt");
    }

    @Test
    @DisplayName("a session is expired the moment it is created when the window is minimal")
    void minimalWindowIsStillValidState() {
        UserSession session = UserSession.open(ID, ID_USER, "token", CREATED_AT,
                CREATED_AT.plusSeconds(1), null, null);

        assertThat(session.isActive(CREATED_AT)).isTrue();
        assertThat(session.isExpired(CREATED_AT.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("refuses a blank refresh token")
    void refusesBlankRefreshToken() {
        assertThatThrownBy(() -> UserSession.open(ID, ID_USER, "  ", CREATED_AT, EXPIRATION_AT, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refreshToken");
    }

    @Test
    @DisplayName("client data may be absent")
    void clientDataIsOptional() {
        UserSession session = UserSession.open(ID, ID_USER, "token", CREATED_AT, EXPIRATION_AT, null, null);

        assertThat(session.ipAddress()).isEmpty();
        assertThat(session.userAgent()).isEmpty();
        assertThat(session.isActive(CREATED_AT)).isTrue();
    }

    @Test
    @DisplayName("identity is the session identifier")
    void equalityIsByIdentifier() {
        UserSession one = openSession();
        UserSession other = new UserSession(ID, "USR9999999", "otro-token", CREATED_AT, EXPIRATION_AT,
                null, null, false, null);

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    }
}
