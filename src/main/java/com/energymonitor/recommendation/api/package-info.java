/**
 * Public API of the Recommendations bounded context.
 *
 * <p>Carries the {@code RecommendationCreated} event the {@code notification} module subscribes
 * to, and the enums that ride inside it. Other modules depend on {@code recommendation::api}.
 */
@NamedInterface
package com.energymonitor.recommendation.api;

import org.springframework.modulith.NamedInterface;
