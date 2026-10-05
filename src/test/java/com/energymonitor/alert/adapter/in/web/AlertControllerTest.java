package com.energymonitor.alert.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.alert.api.AlertStatus;
import com.energymonitor.alert.api.AlertType;
import com.energymonitor.alert.application.exception.AlertNotFoundException;
import com.energymonitor.alert.application.port.in.GetAlert;
import com.energymonitor.alert.application.port.in.ListAlerts;
import com.energymonitor.alert.application.port.in.ResolveAlert;
import com.energymonitor.alert.application.result.AlertResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of the alert endpoints.
 *
 * <p>Filters are disabled because the slice imports the security auto-configurations,
 * which would otherwise demand a JWT. {@code Clock} is mocked because the slice also
 * instantiates the security module's {@code SecurityExceptionHandler}, which requires
 * one (a known slice-isolation gap shared with the home web tests).
 */
@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertControllerTest {

    private static final Instant DATE_TIME = Instant.parse("2026-01-15T10:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAlerts listAlerts;

    @MockitoBean
    private GetAlert getAlert;

    @MockitoBean
    private ResolveAlert resolveAlert;

    @MockitoBean
    private Clock clock;

    private AlertResult sampleResult() {
        return new AlertResult("ale0000001", "hom0000001", "dev0000001", AlertType.THRESHOLD,
                "alert.threshold.high", DATE_TIME, AlertStatus.PENDING, "high000001", "mea0000001");
    }

    @Test
    void listReturns200() throws Exception {
        when(listAlerts.list(any())).thenReturn(List.of(sampleResult()));

        mockMvc.perform(get("/api/v1/alerts").param("homeId", "hom0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAlert").value("ale0000001"))
                .andExpect(jsonPath("$[0].type").value("THRESHOLD"))
                .andExpect(jsonPath("$[0].alertStatus").value("PENDING"));
    }

    @Test
    void listWithStatusFilterReturns200() throws Exception {
        when(listAlerts.list(any())).thenReturn(List.of(sampleResult()));

        mockMvc.perform(get("/api/v1/alerts")
                        .param("homeId", "hom0000001")
                        .param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void getReturns200() throws Exception {
        when(getAlert.get(any())).thenReturn(sampleResult());

        mockMvc.perform(get("/api/v1/alerts/ale0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageKey").value("alert.threshold.high"))
                .andExpect(jsonPath("$.measurementId").value("mea0000001"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(getAlert.get(any())).thenThrow(new AlertNotFoundException("no alert ale0000009"));

        mockMvc.perform(get("/api/v1/alerts/ale0000009"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no alert ale0000009"));
    }

    @Test
    void resolveReturns200() throws Exception {
        when(resolveAlert.resolve(any())).thenReturn(new AlertResult("ale0000001", "hom0000001",
                "dev0000001", AlertType.THRESHOLD, "alert.threshold.high", DATE_TIME,
                AlertStatus.RESOLVED, "high000001", "mea0000001"));

        mockMvc.perform(put("/api/v1/alerts/ale0000001/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertStatus").value("RESOLVED"));
    }

    @Test
    void resolveReturns404WhenMissing() throws Exception {
        when(resolveAlert.resolve(any())).thenThrow(new AlertNotFoundException("no alert ale0000009"));

        mockMvc.perform(put("/api/v1/alerts/ale0000009/resolve"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolveReturns409WhenAlreadyResolved() throws Exception {
        when(resolveAlert.resolve(any()))
                .thenThrow(new IllegalStateException("alert ale0000001 is already resolved"));

        mockMvc.perform(put("/api/v1/alerts/ale0000001/resolve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("alert ale0000001 is already resolved"));
    }
}
