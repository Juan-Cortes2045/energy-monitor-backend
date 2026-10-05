package com.energymonitor.measurement.adapter.out.generator;

import com.energymonitor.measurement.application.port.out.IdentifierGeneratorPort;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates {@code VARCHAR(10)} business identifiers using {@link SecureRandom}.
 *
 * <p>Format: 10 characters from the alphabet {@code ABCDEFGHJKMNPQRSTUVWXYZ23456789}
 * (no ambiguous characters: no 0/O, 1/I/L).
 *
 * <p>Collision risk: 32^10 ≈ 1.1 × 10^15 possible values. With 1000 existing rows,
 * the probability of collision is approximately 1 in 1.1 × 10^12 per generation.
 * The primary key is the final guarantee: a collision would cause a constraint violation.
 */
@Component
public class IdentifierGeneratorAdapter implements IdentifierGeneratorPort {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 10;
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
