package com.energymonitor.measurement.adapter.in.web;

import com.energymonitor.measurement.adapter.in.web.dto.ConsumptionStatisticsResponse;
import com.energymonitor.measurement.adapter.in.web.dto.MeasurementResponse;
import com.energymonitor.measurement.adapter.in.web.dto.RegisterMeasurementRequest;
import com.energymonitor.measurement.application.command.GetConsumptionStatisticsQuery;
import com.energymonitor.measurement.application.command.GetLatestMeasurementQuery;
import com.energymonitor.measurement.application.command.ListMeasurementsQuery;
import com.energymonitor.measurement.application.command.RegisterMeasurementCommand;
import com.energymonitor.measurement.application.port.in.GetConsumptionStatistics;
import com.energymonitor.measurement.application.port.in.GetLatestMeasurement;
import com.energymonitor.measurement.application.port.in.ListMeasurements;
import com.energymonitor.measurement.application.port.in.RegisterMeasurement;
import com.energymonitor.measurement.application.result.ConsumptionStatisticsResult;
import com.energymonitor.measurement.application.result.MeasurementResult;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for measurement ingestion and consumption history.
 *
 * <p>The device is a query parameter rather than a path segment because the Devices
 * bounded context does not exist yet; once it exposes its API these endpoints can be
 * reconsidered under a device-scoped path.
 *
 * <p><strong>Authentication:</strong> the ingestion endpoint will be called by devices
 * authenticated with their {@code api_key}. Device authentication is out of scope until
 * the device module lands, exactly as the MER defers the {@code device_id} validation to
 * application level.
 */
@RestController
@RequestMapping("/api/v1/measurements")
public class MeasurementController {

    private final RegisterMeasurement registerMeasurement;
    private final ListMeasurements listMeasurements;
    private final GetLatestMeasurement getLatestMeasurement;
    private final GetConsumptionStatistics getConsumptionStatistics;

    public MeasurementController(RegisterMeasurement registerMeasurement,
                                 ListMeasurements listMeasurements,
                                 GetLatestMeasurement getLatestMeasurement,
                                 GetConsumptionStatistics getConsumptionStatistics) {
        this.registerMeasurement = registerMeasurement;
        this.listMeasurements = listMeasurements;
        this.getLatestMeasurement = getLatestMeasurement;
        this.getConsumptionStatistics = getConsumptionStatistics;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementResponse register(@Valid @RequestBody RegisterMeasurementRequest request) {
        MeasurementResult result = registerMeasurement.register(new RegisterMeasurementCommand(
                request.deviceId(), request.dateTime(), request.voltage(), request.current(),
                request.activePower(), request.storedEnergy()));
        return toResponse(result);
    }

    @GetMapping
    public List<MeasurementResponse> list(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return listMeasurements.list(new ListMeasurementsQuery(deviceId, from, to)).stream()
                .map(MeasurementController::toResponse)
                .toList();
    }

    @GetMapping("/latest")
    public MeasurementResponse latest(@RequestParam String deviceId) {
        return toResponse(getLatestMeasurement.get(new GetLatestMeasurementQuery(deviceId)));
    }

    @GetMapping("/statistics")
    public ConsumptionStatisticsResponse statistics(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
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
