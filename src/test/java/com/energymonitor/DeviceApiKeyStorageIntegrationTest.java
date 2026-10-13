package com.energymonitor;

import static org.assertj.core.api.Assertions.assertThat;

import com.energymonitor.device.api.DeviceApi;
import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.domain.model.ApiKeyHash;
import com.energymonitor.device.domain.model.Device;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Device API keys against the real schema (after device-007/008): only the hash reaches the table,
 * the broker login works with the key in clear, and anything else is refused.
 *
 * <p>{@code @Transactional}: the test's rows are rolled back.
 */
@SpringBootTest
@Transactional
class DeviceApiKeyStorageIntegrationTest extends JwtKeyedTest {

    private static final String KEY = "0f1e2d3c4b5a69788796a5b4c3d2e1f00f1e2d3c4b5a69788796a5b4c3d2e1f0";

    @Autowired
    private DevicePersistencePort devices;
    @Autowired
    private DeviceApi deviceApi;
    @Autowired
    private AuthorizeBrokerAccess broker;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        devices.save(Device.create("akd0000001", "Nevera", "appl000001", "kitchen", null,
                Instant.parse("2026-03-01T12:00:00Z"), "AK0001", KEY));
        // Written to the table now, so the plain SQL below reads what JPA stored.
        entityManager.flush();
    }

    @Test
    @DisplayName("the table holds the SHA-256 of the key, never the key, and has no clear column")
    void onlyTheHashIsStored() {
        String stored = jdbc.queryForObject("SELECT api_key_hash FROM device WHERE id_device = ?",
                String.class, "akd0000001");

        assertThat(stored).isNotEqualTo(KEY).isEqualTo(ApiKeyHash.of(KEY)).hasSize(64);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM device WHERE api_key_hash LIKE ?",
                Integer.class, "%" + KEY + "%")).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'device' AND column_name = 'api_key'
                """, Integer.class)).isZero();
    }

    @Test
    @DisplayName("MySQL's SHA2 gives the same hash, so the keys migrated by device-007 keep working")
    void migrationHashMatchesTheApplication() {
        assertThat(jdbc.queryForObject("SELECT SHA2(?, 256)", String.class, KEY)).isEqualTo(ApiKeyHash.of(KEY));
    }

    @Test
    @DisplayName("the right key logs in; a wrong key, the hash itself or another code are refused")
    void authentication() {
        assertThat(broker.authenticate("AK0001", KEY)).isTrue();
        assertThat(deviceApi.validateCredentials("AK0001", KEY)).isTrue();

        assertThat(broker.authenticate("AK0001", KEY.substring(1) + "0")).isFalse();
        assertThat(broker.authenticate("AK0001", ApiKeyHash.of(KEY))).isFalse();
        assertThat(broker.authenticate("AK0002", KEY)).isFalse();
        assertThat(deviceApi.validateCredentials("AK0001", "wrong")).isFalse();
    }
}
