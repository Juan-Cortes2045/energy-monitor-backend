package com.energymonitor.security.infrastructure;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Builds the JWT issuing and validating components.
 *
 * <p>Both sides are declared here so they always agree on the algorithm and the key: a token
 * this application signs is a token its own decoder accepts. HS256 over an HMAC key keeps the
 * setup symmetric and single-sourced, which is the right trade-off for one service issuing
 * its own tokens; a resource server consuming a third party's tokens would use JWK sets
 * instead.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class JwtConfiguration {

    /**
     * HMAC-SHA256 needs a key at least as long as the digest it produces.
     */
    private static final int MINIMUM_SECRET_BYTES = 32;

    /**
     * Signs outgoing access tokens.
     *
     * @param properties the configured secret and issuer
     * @return an encoder bound to the configured key
     */
    @Bean
    public JwtEncoder jwtEncoder(SecurityJwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(secretKey(properties))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Verifies incoming access tokens: signature, issuer, expiry and not-before.
     *
     * <p>Validation happens before a request reaches any controller, which is what makes the
     * protected endpoints actually protected rather than merely annotated.
     *
     * @param properties the configured secret and issuer
     * @return a decoder bound to the configured key and issuer
     */
    @Bean
    public JwtDecoder jwtDecoder(SecurityJwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    private SecretKey secretKey(SecurityJwtProperties properties) {
        byte[] secret = properties.secretBytes();
        if (secret == null || secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret must be set to at least " + MINIMUM_SECRET_BYTES
                            + " bytes; supply it through the SECURITY_JWT_SECRET environment"
                            + " variable or application-local.yaml");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }
}
