package com.energymonitor.recommendation.application.port.out;

import com.energymonitor.recommendation.domain.model.HourSample;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Hourly consumption, read from the measurement module.
 */
public interface ConsumptionReaderPort {

    List<HourSample> hourly(Collection<String> deviceIds, Instant from, Instant to);

    List<String> devicesReportingSince(Instant since);
}
