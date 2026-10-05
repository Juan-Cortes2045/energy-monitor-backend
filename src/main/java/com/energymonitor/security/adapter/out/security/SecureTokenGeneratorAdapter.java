package com.energymonitor.security.adapter.out.security;

import com.energymonitor.security.application.port.out.TokenGeneratorPort;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Cryptographically secure implementation of {@link TokenGeneratorPort}.
 *
 * <p>One adapter serves both consumers of the port, password-reset tokens and session
 * refresh tokens, so the generated length is the smaller of the two budgets: 32 random
 * bytes encoded as Base64URL give 43 characters, which fits the roomier
 * {@code user_session.refresh_token VARCHAR(255)}. No column holds a reset token at all
 * any more - only its hash - so that budget is no longer the binding one, and the length is
 * kept uniform because a secret that varies in size with its purpose is one more thing to get
 * wrong.
 *
 * <p>Base64URL is used rather than the standard alphabet because these values travel in
 * URLs, cookies and headers, where {@code +}, {@code /} and {@code =} would need escaping.
 * The output carries 256 bits of entropy from {@link SecureRandom}, so tokens are
 * unguessable rather than merely unique.
 */
@Component
public class SecureTokenGeneratorAdapter implements TokenGeneratorPort {

    /**
     * Bytes of entropy per token. 32 bytes = 256 bits, encoded as 43 Base64URL characters.
     */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
