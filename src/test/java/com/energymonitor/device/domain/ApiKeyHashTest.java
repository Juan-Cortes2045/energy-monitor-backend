package com.energymonitor.device.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.device.domain.model.ApiKeyHash;
import com.energymonitor.device.domain.model.Device;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiKeyHashTest {

    @Test
    void isTheHexSha256OfTheKey() {
        // Known vector: SHA-256("abc").
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ApiKeyHash.of("abc"));
    }

    @Test
    void matchesOnlyTheSameKey() {
        String stored = ApiKeyHash.of("device-key");

        assertTrue(ApiKeyHash.matches("device-key", stored));
        assertFalse(ApiKeyHash.matches("device-kez", stored));
        assertFalse(ApiKeyHash.matches(stored, stored));
        assertFalse(ApiKeyHash.matches("", stored));
        assertFalse(ApiKeyHash.matches(null, stored));
    }

    @Test
    void aDeviceKeepsOnlyTheHashOfTheKeyItIsGiven() {
        Device device = Device.create("dev0000001", "Nevera", "appl000001", null, null,
                Instant.parse("2026-10-01T00:00:00Z"), "EM0001", "first-key");

        assertNotEquals("first-key", device.apiKeyHash());
        assertTrue(device.acceptsApiKey("first-key"));

        Device rotated = device.withApiKey("second-key");
        assertFalse(rotated.acceptsApiKey("first-key"));
        assertTrue(rotated.acceptsApiKey("second-key"));
    }
}
