package com.energymonitor;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies that the module boundaries hold.
 *
 * <p><strong>This does not start the application context.</strong> It reads the compiled classes and
 * the package structure, so nothing here touches a datasource, runs Liquibase or validates a schema.
 * A migration that only fails against a real database will pass this test; one that changes the
 * Liquibase chain passes it too, as long as the code that reads it still compiles.
 *
 * <p>It is the wrong test to reach for when the question is "does the schema apply". For that, run a
 * test annotated with {@code @SpringBootTest}, or start the application: those are what apply the
 * changesets and check the mapping with {@code ddl-auto=validate}.
 */
public class ModularityTests {

    static final ApplicationModules MODULES =
            ApplicationModules.of(EnergyMonitorBackendApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        MODULES.verify();
    }

    @Test
    void printsDetectedModules() {
        MODULES.forEach(System.out::println);
    }
}