/**
 * Public API of the Monitoring and Measurements bounded context.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}. Other modules should depend on {@code measurement::api}
 * instead of {@code measurement} directly.
 *
 * <p>The package also carries the integration surface planned for the {@code alert}
 * and {@code recommendation} modules: the {@code RiskConsumption} enum they need to
 * classify readings and the {@code MeasurementRecorded} event they will subscribe to.
 */
@NamedInterface
package com.energymonitor.measurement.api;

import org.springframework.modulith.NamedInterface;
