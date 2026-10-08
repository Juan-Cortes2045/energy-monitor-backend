package com.energymonitor.recommendation.application.port.out;

import com.energymonitor.recommendation.domain.model.ConsumptionLimit;
import com.energymonitor.recommendation.domain.model.MonitoredDevice;
import java.util.List;
import java.util.Optional;

/**
 * Homes, members, devices and limits, read from the home and device modules.
 */
public interface HomeReaderPort {

    Optional<String> homeOfDevice(String deviceId);

    List<MonitoredDevice> devicesOf(String homeId);

    Optional<ConsumptionLimit> limitOf(String homeId);

    boolean isMember(String userId, String homeId);

    Optional<String> deviceName(String deviceId);
}
