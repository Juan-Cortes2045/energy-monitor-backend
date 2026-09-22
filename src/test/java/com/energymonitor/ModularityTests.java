package com.energymonitor;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {

    static final ApplicationModules MODULES= ApplicationModules.of(EnergyMonitorBackendApplication.class);
    
    @Test 
    void verifiesModuleBoundaries(){
        MODULES.verify();
    }

    @Test 
    void printsDetectedModules(){
        MODULES.forEach(System.out::println);
    }
}