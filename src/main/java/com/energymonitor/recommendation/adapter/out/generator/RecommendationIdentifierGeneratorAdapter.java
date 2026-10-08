package com.energymonitor.recommendation.adapter.out.generator;

import com.energymonitor.recommendation.application.port.out.RecommendationIdentifierPort;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates {@code VARCHAR(10)} identifiers from {@code ABCDEFGHJKMNPQRSTUVWXYZ23456789} (no
 * ambiguous characters), like the other modules. The primary key is the final guard against a
 * collision. Prefixed name: Spring derives bean names from the simple class name.
 */
@Component
public class RecommendationIdentifierGeneratorAdapter implements RecommendationIdentifierPort {

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
