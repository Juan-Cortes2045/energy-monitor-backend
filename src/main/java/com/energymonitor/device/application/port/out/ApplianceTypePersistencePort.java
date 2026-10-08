package com.energymonitor.device.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the appliance type catalog.
 */
public interface ApplianceTypePersistencePort {

    /**
     * An entry of the catalog.
     */
    record ApplianceTypeEntry(String idApplianceType, String name) {
    }

    boolean exists(String applianceTypeId);

    Optional<String> findIdByName(String name);

    List<ApplianceTypeEntry> findAll();
}
