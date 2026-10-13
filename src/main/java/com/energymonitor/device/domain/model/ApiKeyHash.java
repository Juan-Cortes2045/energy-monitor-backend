package com.energymonitor.device.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The one-way hash under which a device API key is stored: SHA-256, as 64 lowercase hex characters.
 *
 * <p><strong>Why SHA-256 and not bcrypt/Argon2.</strong> A slow, salted hash protects secrets a
 * person chose, which can be guessed. A device key is not chosen: it is 32 bytes from
 * {@code SecureRandom} (256 bits), so guessing it is out of reach however fast each attempt is, and
 * no two keys are alike, so a salt adds nothing. A slow hash would only make the broker pay ~100 ms
 * on every device login. The same value is what MySQL's {@code SHA2(key, 256)} returns, which is
 * how the migration hashed the keys stored before.
 *
 * <p>Comparisons run in constant time ({@link MessageDigest#isEqual}).
 */
public final class ApiKeyHash {

    /** Length of a stored hash. */
    public static final int LENGTH = 64;

    private ApiKeyHash() {
    }

    /**
     * @param apiKey the key in clear, as handed to the device
     * @return the value to store
     */
    public static String of(String apiKey) {
        Preconditions.text(apiKey, "apiKey");
        return HexFormat.of().formatHex(sha256().digest(apiKey.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Whether a key presented by a device is the one whose hash is stored.
     *
     * @param candidate  the key received, in clear; {@code null} or blank never matches
     * @param storedHash the stored hash
     * @return {@code true} when they match
     */
    public static boolean matches(String candidate, String storedHash) {
        if (candidate == null || candidate.isBlank() || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(of(candidate).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform must provide SHA-256.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
