package com.energymonitor.home.adapter.in.internal;

import com.energymonitor.home.application.port.in.DescribeHomeAccess;
import com.energymonitor.home.application.port.in.DescribeHomeAccess.HomeAccess;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asked by the device service before it lists or changes the devices of a home: always 200 with
 * {@code {"exists":..,"member":..,"owner":..}}, so a missing home is an answer and not an error.
 * Only the services reach it; see {@code InternalApiSecurityConfiguration}.
 */
@Hidden
@RestController
public class HomeAccessController {

    private final DescribeHomeAccess describeHomeAccess;

    public HomeAccessController(DescribeHomeAccess describeHomeAccess) {
        this.describeHomeAccess = describeHomeAccess;
    }

    @GetMapping("/internal/homes/{homeId}/access")
    public HomeAccess access(@PathVariable String homeId, @RequestParam String userId) {
        return describeHomeAccess.describe(userId, homeId);
    }
}
