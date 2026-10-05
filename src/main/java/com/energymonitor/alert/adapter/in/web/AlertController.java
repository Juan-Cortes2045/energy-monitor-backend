package com.energymonitor.alert.adapter.in.web;

import com.energymonitor.alert.adapter.in.web.dto.AlertResponse;
import com.energymonitor.alert.api.AlertStatus;
import com.energymonitor.alert.application.command.GetAlertQuery;
import com.energymonitor.alert.application.command.ListAlertsQuery;
import com.energymonitor.alert.application.command.ResolveAlertCommand;
import com.energymonitor.alert.application.port.in.GetAlert;
import com.energymonitor.alert.application.port.in.ListAlerts;
import com.energymonitor.alert.application.port.in.ResolveAlert;
import com.energymonitor.alert.application.result.AlertResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the alert inbox.
 *
 * <p>There is deliberately no POST: alerts are raised by the system (the
 * {@code MeasurementRecorded} listener today, a connectivity listener when the Devices
 * module lands), never by clients.
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final ListAlerts listAlerts;
    private final GetAlert getAlert;
    private final ResolveAlert resolveAlert;

    public AlertController(ListAlerts listAlerts, GetAlert getAlert, ResolveAlert resolveAlert) {
        this.listAlerts = listAlerts;
        this.getAlert = getAlert;
        this.resolveAlert = resolveAlert;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam String homeId,
                                    @RequestParam(required = false) AlertStatus status) {
        return listAlerts.list(new ListAlertsQuery(homeId, status)).stream()
                .map(AlertController::toResponse)
                .toList();
    }

    @GetMapping("/{idAlert}")
    public AlertResponse get(@PathVariable String idAlert) {
        return toResponse(getAlert.get(new GetAlertQuery(idAlert)));
    }

    @PutMapping("/{idAlert}/resolve")
    public AlertResponse resolve(@PathVariable String idAlert) {
        return toResponse(resolveAlert.resolve(new ResolveAlertCommand(idAlert)));
    }

    private static AlertResponse toResponse(AlertResult result) {
        return new AlertResponse(result.idAlert(), result.homeId(), result.deviceId(),
                result.type(), result.messageKey(), result.dateTime(), result.alertStatus(),
                result.consumptionLevelId(), result.measurementId());
    }
}
