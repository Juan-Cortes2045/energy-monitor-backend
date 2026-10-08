package com.energymonitor.measurement.adapter.in.web;

import com.energymonitor.measurement.adapter.in.web.dto.HomeConsumptionHistoryResponse;
import com.energymonitor.measurement.adapter.in.web.dto.HomeConsumptionSummaryResponse;
import com.energymonitor.measurement.application.port.in.GetHomeConsumption;
import com.energymonitor.measurement.application.port.in.GetHomeConsumption.Period;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumption of a home for its members.
 *
 * <p>{@code zone} is the IANA time zone of the viewer (e.g. {@code America/Bogota}); it decides
 * where "today" and "this month" start. Defaults to UTC.
 */
@RestController
@RequestMapping("/api/v1/homes/{homeId}/consumption")
public class HomeConsumptionController {

    private final GetHomeConsumption getHomeConsumption;
    private final CurrentUserResolver currentUser;

    public HomeConsumptionController(GetHomeConsumption getHomeConsumption, CurrentUserResolver currentUser) {
        this.getHomeConsumption = getHomeConsumption;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public HomeConsumptionSummaryResponse summary(@PathVariable String homeId,
                                                  @RequestParam(defaultValue = "UTC") String zone) {
        return HomeConsumptionSummaryResponse.from(
                getHomeConsumption.summary(currentUser.resolveCurrentUserId(), homeId, zone(zone)));
    }

    @GetMapping("/history")
    public HomeConsumptionHistoryResponse history(@PathVariable String homeId,
                                                  @RequestParam(defaultValue = "month") String period,
                                                  @RequestParam(defaultValue = "UTC") String zone) {
        Period parsed;
        try {
            parsed = Period.valueOf(period.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("period must be day, week, month or year");
        }
        return HomeConsumptionHistoryResponse.from(
                getHomeConsumption.history(currentUser.resolveCurrentUserId(), homeId, parsed, zone(zone)));
    }

    private static ZoneId zone(String zone) {
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("unknown time zone: " + zone);
        }
    }
}
