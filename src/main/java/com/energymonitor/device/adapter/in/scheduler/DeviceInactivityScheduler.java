package com.energymonitor.device.adapter.in.scheduler;

import com.energymonitor.device.application.port.in.DetectInactiveDevices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically marks as {@code OFFLINE} the devices that stopped reporting.
 *
 * <p>The interval is {@code device.connectivity.check-interval}; the inactivity threshold is
 * {@code device.connectivity.offline-after}.
 */
@Component
public class DeviceInactivityScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceInactivityScheduler.class);

    private final DetectInactiveDevices detectInactiveDevices;

    public DeviceInactivityScheduler(DetectInactiveDevices detectInactiveDevices) {
        this.detectInactiveDevices = detectInactiveDevices;
    }

    @Scheduled(fixedDelayString = "${device.connectivity.check-interval:PT30S}",
               initialDelayString = "${device.connectivity.check-interval:PT30S}")
    public void markInactiveDevicesOffline() {
        try {
            int marked = detectInactiveDevices.markInactiveDevicesOffline();
            if (marked > 0) {
                log.info("Marked {} inactive device(s) as OFFLINE", marked);
            }
        } catch (RuntimeException e) {
            log.warn("Inactive device check failed: {}", e.getMessage());
        }
    }
}
