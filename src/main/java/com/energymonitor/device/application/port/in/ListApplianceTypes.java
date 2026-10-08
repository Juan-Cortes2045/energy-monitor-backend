package com.energymonitor.device.application.port.in;

import com.energymonitor.device.application.port.out.ApplianceTypePersistencePort.ApplianceTypeEntry;
import java.util.List;

public interface ListApplianceTypes {

    List<ApplianceTypeEntry> list();
}
