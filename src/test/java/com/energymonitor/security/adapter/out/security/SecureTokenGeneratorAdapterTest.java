package com.energymonitor.security.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the secure token generator.
 *
 * <p>Reset tokens and session refresh tokens are bearer credentials: whoever holds one can
 * take over the account. These tests therefore check not just that tokens differ, but that
 * the output is unguessable in shape and safe to transport.
 */
class SecureTokenGeneratorAdapterTest {

    /** {@code password_reset_token.reset_token} is the stricter of the two token columns. */
    private static final int NARROWEST_TOKEN_COLUMN = 100;

    private static final int SAMPLES = 500;

    private final SecureTokenGeneratorAdapter generator = new SecureTokenGeneratorAdapter();

    @Test
    void producesTheSameLengthEveryTime() {
        String reference = generator.generateToken();

        for (int sample = 0; sample < SAMPLES; sample++) {
            assertEquals(reference.length(), generator.generateToken().length(),
                    "A variable length would let an attacker learn the token's entropy");
        }
    }

    @Test
    void producesAValueThatFitsTheNarrowestTokenColumn() {
        assertTrue(generator.generateToken().length() <= NARROWEST_TOKEN_COLUMN);
    }

    @Test
    void usesOnlyCharactersThatSurviveUrlsCookiesAndHeaders() {
        // Base64URL without padding: no '+', '/' or '=' to escape anywhere in the stack.
        String token = generator.generateToken();

        assertTrue(token.matches("[A-Za-z0-9_-]+"),
                "Token contains a character that would need escaping: " + token);
    }

    @Test
    void producesNoCollisionsAcrossManySamples() {
        Set<String> tokens = new HashSet<>();

        for (int sample = 0; sample < SAMPLES; sample++) {
            tokens.add(generator.generateToken());
        }

        assertEquals(SAMPLES, tokens.size(), "A repeated token would let one holder unlock another session");
    }

    @Test
    void doesNotRepeatBackToBack() {
        // A weak or seeded generator tends to repeat within a small window.
        String previous = generator.generateToken();

        for (int sample = 0; sample < 200; sample++) {
            String current = generator.generateToken();
            assertNotEquals(previous, current);
            previous = current;
        }
    }

    @Test
    void drawsFromTheWholeAlphabetRatherThanAShortRun() {
        // Guards against a generator that produces plausible-looking but low-entropy output,
        // for example by incrementing a counter.
        Set<Character> seen = new HashSet<>();

        for (int sample = 0; sample < SAMPLES; sample++) {
            generator.generateToken().chars().forEach(character -> seen.add((char) character));
        }

        assertTrue(seen.size() >= 40,
                "Only " + seen.size() + " distinct characters appeared, which suggests low entropy");
    }
}
