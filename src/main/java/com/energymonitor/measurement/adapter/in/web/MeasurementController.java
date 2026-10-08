package com.energymonitor.measurement.adapter.in.web;

import com.energymonitor.measurement.adapter.in.web.dto.ConsumptionStatisticsResponse;
import com.energymonitor.measurement.adapter.in.web.dto.MeasurementResponse;
import com.energymonitor.measurement.application.command.GetConsumptionStatisticsQuery;
import com.energymonitor.measurement.application.command.GetLatestMeasurementQuery;
import com.energymonitor.measurement.application.command.ListMeasurementsQuery;
import com.energymonitor.measurement.application.port.in.GetConsumptionStatistics;
import com.energymonitor.measurement.application.port.in.GetLatestMeasurement;
import com.energymonitor.measurement.application.port.in.ListMeasurements;
import com.energymonitor.measurement.application.port.in.AuthorizeDeviceRead;
import com.energymonitor.measurement.application.result.ConsumptionStatisticsResult;
import com.energymonitor.measurement.application.result.MeasurementResult;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Measurement history of a single device, for members of the home it is linked to.
 *
 * <p>There is no POST: devices publish over MQTT ({@code TelemetryMqttConsumer}), authenticated by
 * the broker with their API key. A REST ingestion endpoint would let any logged-in user write
 * readings for any device.
 */
@RestController
@RequestMapping("/api/v1/measurements")
public class MeasurementController {

    private final ListMeasurements listMeasurements;
    private final GetLatestMeasurement getLatestMeasurement;
    private final GetConsumptionStatistics getConsumptionStatistics;
    private final AuthorizeDeviceRead authorizeDeviceRead;
    private final CurrentUserResolver currentUser;

    public MeasurementController(ListMeasurements listMeasurements,
                                 GetLatestMeasurement getLatestMeasurement,
                                 GetConsumptionStatistics getConsumptionStatistics,
                                 AuthorizeDeviceRead authorizeDeviceRead,
                                 CurrentUserResolver currentUser) {
        this.listMeasurements = listMeasurements;
        this.getLatestMeasurement = getLatestMeasurement;
        this.getConsumptionStatistics = getConsumptionStatistics;
        this.authorizeDeviceRead = authorizeDeviceRead;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<MeasurementResponse> list(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        authorizeDeviceRead.requireReadable(currentUser.resolveCurrentUserId(), deviceId);
        return listMeasurements.list(new ListMeasurementsQuery(deviceId, from, to)).stream()
                .map(MeasurementController::toResponse)
                .toList();
    }

    @GetMapping("/latest")
    public MeasurementResponse latest(@RequestParam String deviceId) {
        authorizeDeviceRead.requireReadable(currentUser.resolveCurrentUserId(), deviceId);
        return toResponse(getLatestMeasurement.get(new GetLatestMeasurementQuery(deviceId)));
    }

    @GetMapping("/statistics")
    public ConsumptionStatisticsResponse statistics(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        authorizeDeviceRead.requireReadable(currentUser.resolveCurrentUserId(), deviceId);
        ConsumptionStatisticsResult result =
                getConsumptionStatistics.get(new GetConsumptionStatisticsQuery(deviceId, from, to));
        return new ConsumptionStatisticsResponse(result.deviceId(), result.from(), result.to(),
                result.measurementCount(), result.averageActivePower(), result.maxActivePower(),
                result.consumedEnergy());
    }

    private static MeasurementResponse toResponse(MeasurementResult result) {
        return new MeasurementResponse(result.idMeasurement(), result.deviceId(), result.dateTime(),
                result.voltage(), result.current(), result.activePower(), result.storedEnergy());
    }
}
