/**
 * Public API of the Monitoring and Measurements bounded context, as this backend sees it.
 *
 * <p>Types in this package are exposed to other modules via Spring Modulith's
 * {@code @NamedInterface}: {@link com.energymonitor.measurement.api.MeasurementApi} to classify a
 * reading and read the energy of devices, and the {@code RiskConsumption} classification.
 */
@NamedInterface
package com.energymonitor.measurement.api;

import org.springframework.modulith.NamedInterface;
