/**
 * Bounded context Monitoring and Measurements, as seen from this backend.
 *
 * <p>The readings, their ingestion from MQTT, the consumption history and statistics and the
 * consumption levels live in the measurement service ({@code en-monitor-measurement-service}).
 * What stays here is what this backend needs of it: reading levels and energy through its
 * internal API ({@code adapter.out.remote}), and hearing every recorded reading
 * ({@code adapter.in.messaging}), which the alert module receives as {@code MeasurementRecorded}.
 *
 * <p>Internal layout: {@code adapter}, {@code infrastructure}. Only the types exposed in
 * {@code api} are visible to other modules.
 */
@ApplicationModule(allowedDependencies = {})
package com.energymonitor.measurement;

import org.springframework.modulith.ApplicationModule;
