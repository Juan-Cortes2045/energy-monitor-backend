package com.energymonitor.measurement.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.measurement.application.exception.MeasurementNotFoundException;
import com.energymonitor.measurement.application.port.in.GetConsumptionStatistics;
import com.energymonitor.measurement.application.port.in.GetLatestMeasurement;
import com.energymonitor.measurement.application.port.in.ListMeasurements;
import com.energymonitor.measurement.application.port.in.RegisterMeasurement;
import com.energymonitor.measurement.application.result.ConsumptionStatisticsResult;
import com.energymonitor.measurement.application.result.MeasurementResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of the measurement endpoints.
 *
 * <p>Filters are disabled because the slice imports the security auto-configurations,
 * which would otherwise demand a JWT. {@code Clock} is mocked because the slice also
 * instantiates the security module's {@code SecurityExceptionHandler}, which requires
 * one (a known slice-isolation gap shared with the home web tests).
 */
@WebMvcTest(MeasurementController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeasurementControllerTest {

    private static final Instant DATE_TIME = Instant.parse("2026-01-15T10:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterMeasurement registerMeasurement;

    @MockitoBean
    private ListMeasurements listMeasurements;

    @MockitoBean
    private GetLatestMeasurement getLatestMeasurement;

    @MockitoBean
    private GetConsumptionStatistics getConsumptionStatistics;

    @MockitoBean
    private Clock clock;

    private MeasurementResult sampleResult() {
        return new MeasurementResult("mea0000001", "dev0000001", DATE_TIME, 120.5, 2.5, 300.0, 1520.75);
    }

    @Test
    void registerReturns201() throws Exception {
        when(registerMeasurement.register(any())).thenReturn(sampleResult());

        mockMvc.perform(post("/api/v1/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "dev0000001",
                                  "dateTime": "2026-01-15T10:30:00Z",
                                  "voltage": 120.5,
                                  "current": 2.5,
                                  "activePower": 300.0,
                                  "storedEnergy": 1520.75
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMeasurement").value("mea0000001"))
                .andExpect(jsonPath("$.deviceId").value("dev0000001"))
                .andExpect(jsonPath("$.activePower").value(300.0));
    }

    @Test
    void registerWithoutDateTimeReturns201() throws Exception {
        when(registerMeasurement.register(any())).thenReturn(sampleResult());

        mockMvc.perform(post("/api/v1/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "dev0000001",
                                  "voltage": 120.5,
                                  "current": 2.5,
                                  "activePower": 300.0,
                                  "storedEnergy": 1520.75
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void registerWithNegativeValueReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "dev0000001",
                                  "voltage": -1,
                                  "current": 2.5,
                                  "activePower": 300.0,
                                  "storedEnergy": 1520.75
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns200() throws Exception {
        when(listMeasurements.list(any())).thenReturn(List.of(sampleResult()));

        mockMvc.perform(get("/api/v1/measurements").param("deviceId", "dev0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idMeasurement").value("mea0000001"));
    }

    @Test
    void latestReturns200() throws Exception {
        when(getLatestMeasurement.get(any())).thenReturn(sampleResult());

        mockMvc.perform(get("/api/v1/measurements/latest").param("deviceId", "dev0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storedEnergy").value(1520.75));
    }

    @Test
    void latestReturns404WhenDeviceHasNoMeasurements() throws Exception {
        when(getLatestMeasurement.get(any()))
                .thenThrow(new MeasurementNotFoundException("no measurements for device dev0000001"));

        mockMvc.perform(get("/api/v1/measurements/latest").param("deviceId", "dev0000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no measurements for device dev0000001"));
    }

    @Test
    void statisticsReturns200() throws Exception {
        when(getConsumptionStatistics.get(any())).thenReturn(new ConsumptionStatisticsResult(
                "dev0000001", DATE_TIME.minusSeconds(3600), DATE_TIME, 3, 200.0, 300.0, 750.0));

        mockMvc.perform(get("/api/v1/measurements/statistics").param("deviceId", "dev0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurementCount").value(3))
                .andExpect(jsonPath("$.averageActivePower").value(200.0))
                .andExpect(jsonPath("$.maxActivePower").value(300.0))
                .andExpect(jsonPath("$.consumedEnergy").value(750.0));
    }
}
