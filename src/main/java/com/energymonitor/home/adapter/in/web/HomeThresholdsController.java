package com.energymonitor.home.adapter.in.web;

import com.energymonitor.home.adapter.in.web.dto.HomeThresholdsResponse;
import com.energymonitor.home.adapter.in.web.dto.UpdateHomeThresholdsRequest;
import com.energymonitor.home.application.command.GetHomeThresholdsQuery;
import com.energymonitor.home.application.command.UpdateHomeThresholdsCommand;
import com.energymonitor.home.application.port.in.GetHomeThresholds;
import com.energymonitor.home.application.port.in.UpdateHomeThresholds;
import com.energymonitor.home.application.result.HomeThresholdsResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for home thresholds endpoints.
 */
@RestController
@RequestMapping("/api/v1/homes/{homeId}/thresholds")
public class HomeThresholdsController {

    private final GetHomeThresholds getHomeThresholds;
    private final UpdateHomeThresholds updateHomeThresholds;
    private final CurrentUserResolver currentUser;

    public HomeThresholdsController(GetHomeThresholds getHomeThresholds,
                                    UpdateHomeThresholds updateHomeThresholds,
                                    CurrentUserResolver currentUser) {
        this.getHomeThresholds = getHomeThresholds;
        this.updateHomeThresholds = updateHomeThresholds;
        this.currentUser = currentUser;
    }

    @GetMapping
    public HomeThresholdsResponse getThresholds(@PathVariable String homeId) {
        String userId = currentUser.resolveCurrentUserId();
        HomeThresholdsResult result = getHomeThresholds.get(new GetHomeThresholdsQuery(userId, homeId));
        return new HomeThresholdsResponse(result.idThreshold(), result.homeId(),
                result.dailyLimit(), result.monthlyLimit(), result.useSystemDefault(), result.limitPeriod(),
                result.defaultDailyLimit(), result.defaultMonthlyLimit());
    }

    /** Applies the system default limits again (the "use system defaults" switch turned on). */
    @PostMapping("/defaults")
    public HomeThresholdsResponse resetToDefaults(@PathVariable String homeId) {
        HomeThresholdsResult result = updateHomeThresholds.resetToDefaults(currentUser.resolveCurrentUserId(),
                homeId);
        return new HomeThresholdsResponse(result.idThreshold(), result.homeId(),
                result.dailyLimit(), result.monthlyLimit(), result.useSystemDefault(), result.limitPeriod(),
                result.defaultDailyLimit(), result.defaultMonthlyLimit());
    }

    @PutMapping
    public HomeThresholdsResponse updateThresholds(@PathVariable String homeId,
                                                   @Valid @RequestBody UpdateHomeThresholdsRequest request) {
        String userId = currentUser.resolveCurrentUserId();
        HomeThresholdsResult result = updateHomeThresholds.update(new UpdateHomeThresholdsCommand(
                userId, homeId, request.limitPeriod(), request.limit()));
        return new HomeThresholdsResponse(result.idThreshold(), result.homeId(),
                result.dailyLimit(), result.monthlyLimit(), result.useSystemDefault(), result.limitPeriod(),
                result.defaultDailyLimit(), result.defaultMonthlyLimit());
    }
}
