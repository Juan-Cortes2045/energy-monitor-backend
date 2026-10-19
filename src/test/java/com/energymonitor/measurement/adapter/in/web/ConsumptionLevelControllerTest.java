package com.energymonitor.measurement.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.measurement.api.RiskConsumption;
import com.energymonitor.measurement.application.port.in.ListConsumptionLevels;
import com.energymonitor.measurement.application.result.ConsumptionLevelResult;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of the consumption level catalog.
 *
 * <p>See {@link MeasurementControllerTest} for why filters are disabled and {@code Clock}
 * is mocked.
 */
@WebMvcTest(ConsumptionLevelController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsumptionLevelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListConsumptionLevels listConsumptionLevels;

    @MockitoBean
    private Clock clock;

    @Test
    void listReturns200() throws Exception {
        when(listConsumptionLevels.list(any())).thenReturn(List.of(
                new ConsumptionLevelResult("low0000001", RiskConsumption.LOW, "low", 0, 500),
                new ConsumptionLevelResult("medi000001", RiskConsumption.MEDIUM, "medium", 500, 1500)));

        mockMvc.perform(get("/api/v1/consumption-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("LOW"))
                .andExpect(jsonPath("$[1].name").value("MEDIUM"))
                .andExpect(jsonPath("$[1].maxLimit").value(1500.0));
    }
}
