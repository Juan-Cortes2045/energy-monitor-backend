package com.energymonitor.security.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the Security JWT issuer.
 *
 * <p>Bound from {@code security.jwt.*}. The signing secret is deliberately declared without
 * a default so the application refuses to start when it is missing, rather than silently
 * falling back to a well-known key. It is supplied through {@code application-local.yaml}
 * for local runs or the {@code SECURITY_JWT_SECRET} environment variable elsewhere.
 *
 * @param secret           the HMAC signing secret; at least 32 bytes for HS256
 * @param issuer           the {@code iss} claim written into every token
 * @param accessTokenTtl   how long an issued access token stays valid
 */
@ConfigurationProperties(prefix = "security.jwt")
public record SecurityJwtProperties(String secret, String issuer, Duration accessTokenTtl) {

    /**
     * @return the secret material as an HMAC key, or {@code null} when unset
     */
    public byte[] secretBytes() {
        return secret == null ? null : secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
