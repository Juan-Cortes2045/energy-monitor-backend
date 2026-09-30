package com.energymonitor.security.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract of the refresh token hasher: deterministic, SHA-256, and never reversible.
 */
@DisplayName("SHA-256 refresh token hasher")
class Sha256RefreshTokenHasherTest {

    private final Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

    @Test
    @DisplayName("the same secret always produces the same hash")
    void isDeterministic() {
        String secret = "un-token-de-refresco-con-256-bits-de-entropia";

        assertThat(hasher.hash(secret)).isEqualTo(hasher.hash(secret));
    }

    @Test
    @DisplayName("different secrets produce different hashes")
    void distinctSecretsDistinctHashes() {
        assertThat(hasher.hash("token-a")).isNotEqualTo(hasher.hash("token-b"));
    }

    @Test
    @DisplayName("the hash is 64 lowercase hex characters, matching the column")
    void hasTheExpectedShape() {
        String hash = hasher.hash("cualquier-secreto");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("the digest really is SHA-256 over the UTF-8 bytes")
    void isSha256OfTheInput() throws Exception {
        String secret = "verificacion-independent-del-adapter";
        byte[] expected = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));

        // Computed independently here so the test cannot pass by agreeing with a bug in the
        // adapter's own encoding.
        assertThat(hasher.hash(secret)).isEqualTo(HexFormat.of().formatHex(expected));
    }

    @Test
    @DisplayName("the hash does not contain the secret")
    void doesNotLeakTheSecret() {
        String secret = "secreto-que-no-debe-aparecer";

        assertThat(hasher.hash(secret)).doesNotContain(secret);
    }

    @Test
    @DisplayName("refuses a blank secret")
    void refusesBlankSecret() {
        assertThatThrownBy(() -> hasher.hash("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses a null secret")
    void refusesNullSecret() {
        assertThatThrownBy(() -> hasher.hash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("hashes near-identical secrets differently, so it is not a trivial encoding")
    void isNotATrivialEncoding() {
        // A concatenation or truncation bug would collapse these two.
        assertThat(hasher.hash("a".repeat(43))).isNotEqualTo(hasher.hash("a".repeat(44)));
    }
}
