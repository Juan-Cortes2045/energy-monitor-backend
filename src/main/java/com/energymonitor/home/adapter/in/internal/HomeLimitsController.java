package com.energymonitor.home.adapter.in.internal;

import com.energymonitor.home.application.port.in.DescribeHomeLimits;
import com.energymonitor.home.application.port.in.DescribeHomeLimits.HomeLimits;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asked by the measurement service for the consumption summary of a home: 200 with
 * {@code {"dailyLimit":..,"monthlyLimit":..,"limitPeriod":..}}, or 404 when the home has none.
 * Only the services reach it; see {@code InternalApiSecurityConfiguration}.
 */
@Hidden
@RestController
public class HomeLimitsController {

    private final DescribeHomeLimits describeHomeLimits;

    public HomeLimitsController(DescribeHomeLimits describeHomeLimits) {
        this.describeHomeLimits = describeHomeLimits;
    }

    @GetMapping("/internal/homes/{homeId}/thresholds")
    public ResponseEntity<HomeLimits> limits(@PathVariable String homeId) {
        return ResponseEntity.of(describeHomeLimits.describe(homeId));
    }
}
