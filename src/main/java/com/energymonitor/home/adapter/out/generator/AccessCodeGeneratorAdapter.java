package com.energymonitor.home.adapter.out.generator;

import com.energymonitor.home.application.port.out.AccessCodeGeneratorPort;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates 8-character access codes using {@link SecureRandom}.
 *
 * <p>Alphabet: uppercase letters excluding ambiguous characters (no 0/O, 1/I/L).
 * This gives 32 possible characters per position, so 32^8 ≈ 1.1 × 10^12 combinations.
 */
@Component
public class AccessCodeGeneratorAdapter implements AccessCodeGeneratorPort {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
